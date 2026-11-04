/**
 * UCO Bank Realty - Project Form Interactive Logic
 * Handles 10-slot photo uploads, dynamic state/district selection, and date guards.
 * Compliance: Zero inline JS (Strict CSP compliance for bank audits)
 */

document.addEventListener("DOMContentLoaded", function () {
    initDistrictSelector();
    initPhotoSlots();
    initDateGuards();
});

/**
 * State to District cascading dropdown
 */
function initDistrictSelector() {
    var stateSelect = document.getElementById("stateName");
    var citySelect = document.getElementById("cityName");
    var btnToggleCustom = document.getElementById("btnToggleCustomCity");
    var customCityContainer = document.getElementById("customCityContainer");
    var customCityInput = document.getElementById("customCityInput");
    var btnCancelCustom = document.getElementById("btnCancelCustomCity");
    var formEl = document.getElementById("projectForm");

    if (!stateSelect || !citySelect) return;

    var geoData = {
        "Andaman and Nicobar": ["Nicobars", "North and Middle Andaman", "South Andaman"],
        "Andhra Pradesh": ["Anantapur", "Chittoor", "East Godavari", "Guntur", "Krishna", "Kurnool", "Prakasam", "Srikakulam", "Sri Potti Sriramulu Nellore", "Visakhapatnam", "Vizianagaram", "West Godavari", "Y.S.R. Kadapa"],
        "Arunachal Pradesh": ["Anjaw", "Changlang", "Dibang Valley", "East Kameng", "East Siang", "Kamle", "Kra Daadi", "Kurung Kumey", "Lepa Rada", "Lohit", "Longding", "Lower Dibang Valley", "Lower Siang", "Lower Subansiri", "Namsai", "Pakke Kessang", "Papum Pare", "Shi Yomi", "Siang", "Tawang", "Tirap", "Upper Siang", "Upper Subansiri", "West Kameng", "West Siang"],
        "Assam": ["Baksa", "Barpeta", "Biswanath", "Bongaigaon", "Cachar", "Charaideo", "Chirang", "Darrang", "Dhemaji", "Dhubri", "Dibrugarh", "Dima Hasao", "Goalpara", "Golaghat", "Hailakandi", "Hojai", "Jorhat", "Kamrup", "Kamrup Metropolitan", "Karbi Anglong", "Karimganj", "Kokrajhar", "Lakhimpur", "Majuli", "Morigaon", "Nagaon", "Nalbari", "Sivasagar", "Sonitpur", "South Salmara-Mankachar", "Tinsukia", "Udalguri", "West Karbi Anglong"],
        "Bihar": ["Araria", "Arwal", "Aurangabad", "Banka", "Begusarai", "Bhagalpur", "Bhojpur", "Buxar", "Darbhanga", "East Champaran", "Gaya", "Gopalganj", "Jamui", "Jehanabad", "Kaimur", "Katihar", "Khagaria", "Kishanganj", "Lakhisarai", "Madhepura", "Madhubani", "Munger", "Muzaffarpur", "Nalanda", "Nawada", "Patna", "Purnia", "Rohtas", "Saharsa", "Samastipur", "Saran", "Sheikhpura", "Sheohar", "Sitamarhi", "Siwan", "Supaul", "Vaishali", "West Champaran"],
        "Chandigarh": ["Chandigarh"],
        "Chhattisgarh": ["Balod", "Baloda Bazar", "Balrampur", "Bastar", "Bemetara", "Bijapur", "Bilaspur", "Dantewada", "Dhamtari", "Durg", "Gariaband", "Gaurela Pendra Marwahi", "Janjgir-Champa", "Jashpur", "Kabirdham", "Kanker", "Kondagaon", "Korba", "Koriya", "Mahasamund", "Mungeli", "Narayanpur", "Raigarh", "Raipur", "Rajnandgaon", "Sukma", "Surajpur", "Surguja"],
        "Dadra and Nagar Haveli and Daman and Diu": ["Dadra and Nagar Haveli", "Daman", "Diu"],
        "Delhi": ["Central Delhi", "East Delhi", "New Delhi", "North Delhi", "North East Delhi", "North West Delhi", "Shahdara", "South Delhi", "South East Delhi", "South West Delhi", "West Delhi"],
        "Goa": ["North Goa", "South Goa"],
        "Gujarat": ["Ahmedabad", "Amreli", "Anand", "Aravalli", "Banaskantha", "Bharuch", "Bhavnagar", "Botad", "Chhota Udaipur", "Dahod", "Dang", "Devbhumi Dwarka", "Gandhinagar", "Gir Somnath", "Jamnagar", "Junagadh", "Kheda", "Kutch", "Mahisagar", "Mehsana", "Morbi", "Narmada", "Navsari", "Panchmahal", "Patan", "Porbandar", "Rajkot", "Sabarkantha", "Surat", "Surendranagar", "Tapi", "Vadodara", "Valsad"],
        "Haryana": ["Ambala", "Bhiwani", "Charkhi Dadri", "Faridabad", "Fatehabad", "Gurugram", "Hisar", "Jhajjar", "Jind", "Kaithal", "Karnal", "Kurukshetra", "Mahendragarh", "Nuh", "Palwal", "Panchkula", "Panipat", "Rewari", "Rohtak", "Sirsa", "Sonipat", "Yamunanagar"],
        "Himachal Pradesh": ["Bilaspur", "Chamba", "Hamirpur", "Kangra", "Kinnaur", "Kullu", "Lahaul and Spiti", "Mandi", "Shimla", "Sirmaur", "Solan", "Una"],
        "Jammu and Kashmir": ["Anantnag", "Bandipora", "Baramulla", "Budgam", "Doda", "Ganderbal", "Jammu", "Kathua", "Kishtwar", "Kulgam", "Kupwara", "Poonch", "Pulwama", "Rajouri", "Ramban", "Reasi", "Samba", "Shopian", "Srinagar", "Udhampur"],
        "Jharkhand": ["Bokaro", "Chatra", "Deoghar", "Dhanbad", "Dumka", "East Singhbhum", "Garhwa", "Giridih", "Godda", "Gumla", "Hazaribagh", "Jamtara", "Khunti", "Koderma", "Latehar", "Lohardaga", "Pakur", "Palamu", "Ramgarh", "Ranchi", "Sahebganj", "Seraikela Kharsawan", "Simdega", "West Singhbhum"],
        "Karnataka": ["Bagalkot", "Ballari", "Belagavi", "Bengaluru Rural", "Bengaluru Urban", "Bidar", "Chamarajanagar", "Chikkaballapura", "Chikkamagaluru", "Chitradurga", "Dakshina Kannada", "Davanagere", "Dharwad", "Gadag", "Hassan", "Haveri", "Kalaburagi", "Kodagu", "Kolar", "Koppal", "Mandya", "Mysuru", "Raichur", "Ramanagara", "Shivamogga", "Tumakuru", "Udupi", "Uttara Kannada", "Vijayapura", "Yadgir"],
        "Kerala": ["Alappuzha", "Ernakulam", "Idukki", "Kannur", "Kasaragod", "Kollam", "Kottayam", "Kozhikode", "Malappuram", "Palakkad", "Pathanamthitta", "Thiruvananthapuram", "Thrissur", "Wayanad"],
        "Ladakh": ["Kargil", "Leh"],
        "Madhya Pradesh": ["Agar Malwa", "Alirajpur", "Anuppur", "Ashoknagar", "Balaghat", "Barwani", "Betul", "Bhind", "Bhopal", "Burhanpur", "Chhatarpur", "Chhindwara", "Damoh", "Datia", "Dewas", "Dhar", "Dindori", "Guna", "Gwalior", "Harda", "Hoshangabad", "Indore", "Jabalpur", "Jhabua", "Katni", "Khandwa", "Khargone", "Mandla", "Mandsaur", "Morena", "Narsinghpur", "Neemuch", "Niwari", "Panna", "Raisen", "Rajgarh", "Ratlam", "Rewa", "Sagar", "Satna", "Sehore", "Seoni", "Shahdol", "Shajapur", "Sheopur", "Shivpuri", "Sidhi", "Singrauli", "Tikamgarh", "Ujjain", "Umaria", "Vidisha"],
        "Maharashtra": ["Ahmednagar", "Akola", "Amravati", "Aurangabad", "Beed", "Bhandara", "Buldhana", "Chandrapur", "Dhule", "Gadchiroli", "Gondia", "Hingoli", "Jalgaon", "Jalna", "Kolhapur", "Latur", "Mumbai City", "Mumbai Suburban", "Nagpur", "Nanded", "Nandurbar", "Nashik", "Osmanabad", "Palghar", "Parbhani", "Pune", "Raigad", "Ratnagiri", "Sangli", "Satara", "Sindhudurg", "Solapur", "Thane", "Wardha", "Washim", "Yavatmal"],
        "Odisha": ["Angul", "Balangir", "Balasore", "Bargarh", "Bhadrak", "Boudh", "Cuttack", "Deogarh", "Dhenkanal", "Gajapati", "Ganjam", "Jagatsinghpur", "Jajpur", "Jharsuguda", "Kalahandi", "Kandhamal", "Kendrapara", "Kendujhar", "Khordha", "Koraput", "Malkangiri", "Mayurbhanj", "Nabarangpur", "Nayagarh", "Nuapada", "Puri", "Rayagada", "Sambalpur", "Subarnapur", "Sundargarh"],
        "Punjab": ["Amritsar", "Barnala", "Bathinda", "Faridkot", "Fatehgarh Sahib", "Fazilka", "Ferozepur", "Gurdaspur", "Hoshiarpur", "Jalandhar", "Kapurthala", "Ludhiana", "Mansa", "Moga", "Muktsar", "Pathankot", "Patiala", "Rupnagar", "Sahibzada Ajit Singh Nagar", "Sangrur", "Shahid Bhagat Singh Nagar", "Sri Muktsar Sahib", "Tarn Taran"],
        "Rajasthan": ["Ajmer", "Alwar", "Banswara", "Baran", "Barmer", "Bharatpur", "Bhilwara", "Bikaner", "Bundi", "Chittorgarh", "Churu", "Dausa", "Dholpur", "Dungarpur", "Hanumangarh", "Jaipur", "Jaisalmer", "Jalore", "Jhalawar", "Jhunjhunu", "Jodhpur", "Karauli", "Kota", "Nagaur", "Pali", "Pratapgarh", "Rajsamand", "Sawai Madhopur", "Sikar", "Sirohi", "Sri Ganganagar", "Tonk", "Udaipur"],
        "Tamil Nadu": ["Ariyalur", "Chengalpattu", "Chennai", "Coimbatore", "Cuddalore", "Dharmapuri", "Dindigul", "Erode", "Kallakurichi", "Kanchipuram", "Kanyakumari", "Karur", "Krishnagiri", "Madurai", "Mayiladuthurai", "Nagapattinam", "Namakkal", "Nilgiris", "Perambalur", "Pudukkottai", "Ramanathapuram", "Ranipet", "Salem", "Sivaganga", "Tenkasi", "Thanjavur", "Theni", "Thoothukudi", "Tiruchirappalli", "Tirunelveli", "Tirupathur", "Tiruppur", "Tiruvallur", "Tiruvannamalai", "Tiruvarur", "Vellore", "Viluppuram", "Virudhunagar"],
        "Telangana": ["Adilabad", "Bhadradri Kothagudem", "Hyderabad", "Jagtial", "Jangaon", "Jayashankar Bhupalpally", "Jogulamba Gadwal", "Kamareddy", "Karimnagar", "Khammam", "Kumuram Bheem Asifabad", "Mahabubabad", "Mahabubnagar", "Mancherial", "Medak", "Medchal Malkajgiri", "Mulugu", "Nagarkurnool", "Nalgonda", "Narayanpet", "Nirmal", "Nizamabad", "Peddapalli", "Rajanna Sircilla", "Rangareddy", "Sangareddy", "Siddipet", "Suryapet", "Vikarabad", "Wanaparthy", "Warangal Rural", "Warangal Urban", "Yadadri Bhuvanagiri"],
        "Uttar Pradesh": ["Agra", "Aligarh", "Ambedkar Nagar", "Amethi", "Amroha", "Auraiya", "Ayodhya", "Azamgarh", "Baghpat", "Bahraich", "Ballia", "Balrampur", "Banda", "Barabanki", "Bareilly", "Basti", "Bhadohi", "Bijnor", "Budaun", "Bulandshahr", "Chandauli", "Chitrakoot", "Deoria", "Etah", "Etawah", "Farrukhabad", "Fatehpur", "Firozabad", "Gautam Buddha Nagar", "Ghaziabad", "Ghazipur", "Gonda", "Gorakhpur", "Hamirpur", "Hapur", "Hardoi", "Hathras", "Jalaun", "Jaunpur", "Jhansi", "Kannauj", "Kanpur Dehat", "Kanpur Nagar", "Kasganj", "Kaushambi", "Kheri", "Kushinagar", "Lalitpur", "Lucknow", "Maharajganj", "Mahoba", "Mainpuri", "Mathura", "Mau", "Meerut", "Mirzapur", "Moradabad", "Muzaffarnagar", "Pilibhit", "Pratapgarh", "Prayagraj", "Raebareli", "Rampur", "Saharanpur", "Sambhal", "Sant Kabir Nagar", "Shahjahanpur", "Shamli", "Shrawasti", "Siddharthnagar", "Sitapur", "Sonbhadra", "Sultanpur", "Unnao", "Varanasi"],
        "Uttarakhand": ["Almora", "Bageshwar", "Chamoli", "Champawat", "Dehradun", "Haridwar", "Nainital", "Pauri Garhwal", "Pithoragarh", "Rudraprayag", "Tehri Garhwal", "Udham Singh Nagar", "Uttarkashi"],
        "West Bengal": ["Alipurduar", "Bankura", "Birbhum", "Cooch Behar", "Dakshin Dinajpur", "Darjeeling", "Hooghly", "Howrah", "Jalpaiguri", "Jhargram", "Kalimpong", "Kolkata", "Malda", "Murshidabad", "Nadia", "North 24 Parganas", "Paschim Bardhaman", "Paschim Medinipur", "Purba Bardhaman", "Purba Medinipur", "Purulia", "South 24 Parganas", "Uttar Dinajpur"]
    };

    function showCustomCityMode(presetValue) {
        if (customCityContainer) {
            customCityContainer.classList.remove("d-none");
        }
        if (customCityInput) {
            if (presetValue !== undefined && presetValue !== null) {
                customCityInput.value = presetValue;
            }
            customCityInput.setAttribute("required", "required");
            customCityInput.focus();
        }
        if (citySelect) {
            citySelect.removeAttribute("required");
            citySelect.value = "__OTHER__";
        }
        if (btnToggleCustom) {
            btnToggleCustom.classList.add("d-none");
        }
    }

    function hideCustomCityMode() {
        if (customCityContainer) {
            customCityContainer.classList.add("d-none");
        }
        if (customCityInput) {
            customCityInput.value = "";
            customCityInput.removeAttribute("required");
        }
        if (citySelect) {
            citySelect.setAttribute("required", "required");
            if (citySelect.value === "__OTHER__") {
                citySelect.value = "";
            }
        }
        if (btnToggleCustom) {
            btnToggleCustom.classList.remove("d-none");
        }
    }

    function updateOptions() {
        var selectedState = stateSelect.value;
        var currentCity = citySelect.getAttribute("data-selected-city") || citySelect.value;
        citySelect.innerHTML = '<option value="">-- Select District / City --</option>';

        var matched = false;
        if (selectedState && geoData[selectedState]) {
            geoData[selectedState].forEach(function (city) {
                var opt = document.createElement("option");
                opt.value = city;
                opt.text = city;
                if (currentCity && currentCity.trim().toLowerCase() === city.toLowerCase()) {
                    opt.selected = true;
                    matched = true;
                }
                citySelect.appendChild(opt);
            });
        }

        var otherOpt = document.createElement("option");
        otherOpt.value = "__OTHER__";
        otherOpt.text = "+ Other / Enter Custom City...";
        citySelect.appendChild(otherOpt);

        if (currentCity && currentCity.trim() !== "" && currentCity !== "__OTHER__") {
            if (!matched) {
                showCustomCityMode(currentCity.trim());
            } else {
                hideCustomCityMode();
            }
        } else if (currentCity === "__OTHER__") {
            showCustomCityMode();
        }
    }

    stateSelect.addEventListener("change", function () {
        var customVal = (customCityInput && customCityContainer && !customCityContainer.classList.contains("d-none"))
            ? customCityInput.value.trim() : null;
        if (customVal) {
            citySelect.setAttribute("data-selected-city", customVal);
        } else {
            citySelect.removeAttribute("data-selected-city");
        }
        updateOptions();
    });

    citySelect.addEventListener("change", function () {
        if (citySelect.value === "__OTHER__") {
            showCustomCityMode();
        } else {
            hideCustomCityMode();
        }
    });

    if (btnToggleCustom) {
        btnToggleCustom.addEventListener("click", function () {
            showCustomCityMode();
        });
    }

    if (btnCancelCustom) {
        btnCancelCustom.addEventListener("click", function () {
            hideCustomCityMode();
            citySelect.focus();
        });
    }

    if (formEl) {
        formEl.addEventListener("submit", function (e) {
            if (customCityContainer && !customCityContainer.classList.contains("d-none")) {
                var val = customCityInput ? customCityInput.value.trim() : "";
                if (!val) {
                    e.preventDefault();
                    alert("Please enter a custom district / city name, or click 'Back to List' to select one.");
                    if (customCityInput) customCityInput.focus();
                    return false;
                }
                var found = false;
                for (var i = 0; i < citySelect.options.length; i++) {
                    if (citySelect.options[i].value.toLowerCase() === val.toLowerCase()) {
                        citySelect.options[i].selected = true;
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    var dynOpt = document.createElement("option");
                    dynOpt.value = val;
                    dynOpt.text = val;
                    dynOpt.selected = true;
                    citySelect.appendChild(dynOpt);
                }
                citySelect.value = val;
            }
        });

        // Runs when the browser builds the data to send (after every other submit check passed); see packTextFields.
        formEl.addEventListener("formdata", function (e) {
            packTextFields(e.formData);
        });
    }

    // Populate initially if state is selected
    if (stateSelect.value) {
        updateOptions();
    }
}

/**
 * 10-Slot Dynamic Photo Uploads
 */
function initPhotoSlots() {
    var addMoreBtn = document.getElementById("addMorePhotosBtn");
    var counterText = document.getElementById("visibleSlotCountText");
    var formEl = document.getElementById("projectForm");
    var contextPath = formEl ? (formEl.getAttribute("data-context-path") || "") : "";

    // 1. Trigger file browser when clicking upload prompt
    document.querySelectorAll(".slot-upload-prompt").forEach(function (prompt) {
        prompt.addEventListener("click", function () {
            var slotIndex = prompt.getAttribute("data-slot");
            var input = document.getElementById("photo_input_" + slotIndex);
            if (input) input.click();
        });
    });

    // 2. Handle file selection, preview, and size validation
    document.querySelectorAll(".slot-file-input").forEach(function (input) {
        input.addEventListener("change", function () {
            var slotIndex = input.getAttribute("data-slot");
            if (!input.files || !input.files[0]) return;

            var file = input.files[0];

            // Size ceiling: 2MB (2 * 1024 * 1024 bytes)
            if (file.size > 2 * 1024 * 1024) {
                alert("File \"" + file.name + "\" exceeds the 2MB limit (" + (file.size / 1024 / 1024).toFixed(2) + " MB). Please choose an image under 2MB.");
                input.value = "";
                return;
            }

            // Type check
            var allowed = ["image/jpeg", "image/png", "image/webp"];
            if (allowed.indexOf(file.type.toLowerCase()) === -1) {
                alert("Only JPG, PNG, and WEBP images are accepted.");
                input.value = "";
                return;
            }

            var promptEl = document.getElementById("upload_prompt_" + slotIndex);
            var previewWrap = document.getElementById("preview_wrap_" + slotIndex);
            var previewImg = document.getElementById("preview_img_" + slotIndex);
            var sizeSpan = document.getElementById("preview_size_" + slotIndex);
            var nameDiv = document.getElementById("preview_name_" + slotIndex);

            var reader = new FileReader();
            reader.onload = function (e) {
                if (previewImg) previewImg.src = e.target.result;
                if (sizeSpan) sizeSpan.innerText = (file.size / 1024).toFixed(0) + " KB";
                if (nameDiv) {
                    nameDiv.innerText = file.name;
                    nameDiv.title = file.name;
                }
                if (promptEl) promptEl.classList.add("d-none");
                if (previewWrap) {
                    previewWrap.classList.remove("d-none");
                    previewWrap.classList.add("d-flex");
                }
            };
            reader.readAsDataURL(file);
        });
    });

    // 3. Clear / Remove un-submitted slot file
    document.querySelectorAll(".slot-clear-btn").forEach(function (btn) {
        btn.addEventListener("click", function () {
            var slotIndex = btn.getAttribute("data-slot");
            var input = document.getElementById("photo_input_" + slotIndex);
            var promptEl = document.getElementById("upload_prompt_" + slotIndex);
            var previewWrap = document.getElementById("preview_wrap_" + slotIndex);
            var previewImg = document.getElementById("preview_img_" + slotIndex);

            if (input) input.value = "";
            if (previewImg) previewImg.src = "";
            if (previewWrap) {
                previewWrap.classList.remove("d-flex");
                previewWrap.classList.add("d-none");
            }
            if (promptEl) promptEl.classList.remove("d-none");
        });
    });

    // 4. Reveal additional photo slots (Slots 5 through 10)
    if (addMoreBtn) {
        addMoreBtn.addEventListener("click", function () {
            for (var i = 1; i <= 10; i++) {
                var card = document.getElementById("slot_card_" + i);
                if (card && card.classList.contains("d-none")) {
                    card.classList.remove("d-none");
                    card.classList.add("d-flex");
                    updateVisibleCounter();
                    break;
                }
            }
        });
    }

    // 5. Hide extra slot (✕ button on slots 5..10)
    document.querySelectorAll(".slot-remove-extra-btn").forEach(function (btn) {
        btn.addEventListener("click", function (e) {
            e.stopPropagation();
            var slotIndex = btn.getAttribute("data-slot");
            var card = document.getElementById("slot_card_" + slotIndex);
            var input = document.getElementById("photo_input_" + slotIndex);
            var promptEl = document.getElementById("upload_prompt_" + slotIndex);
            var previewWrap = document.getElementById("preview_wrap_" + slotIndex);
            var previewImg = document.getElementById("preview_img_" + slotIndex);

            if (input) input.value = "";
            if (previewImg) previewImg.src = "";
            if (previewWrap) {
                previewWrap.classList.remove("d-flex");
                previewWrap.classList.add("d-none");
            }
            if (promptEl) promptEl.classList.remove("d-none");
            if (card) {
                card.classList.remove("d-flex");
                card.classList.add("d-none");
            }
            updateVisibleCounter();
        });
    });

    // 6. Delete saved photo from DB
    document.querySelectorAll(".slot-delete-saved-btn").forEach(function (btn) {
        btn.addEventListener("click", function () {
            var imgId = btn.getAttribute("data-image-id");
            var projectId = btn.getAttribute("data-project-id");
            var context = btn.getAttribute("data-context-path") || "";

            if (!imgId || !projectId) return;
            if (!confirm("Are you sure you want to delete this saved photo?")) return;

            var form = document.createElement("form");
            form.method = "POST";
            // the button carries the URL of the maker's working copy (staging request)
            form.action = btn.getAttribute("data-delete-url");
            if (!form.action) return;
            var csrf = document.querySelector('input[name="_csrf"]');
            if (csrf) {
                var token = document.createElement("input");
                token.type = "hidden";
                token.name = csrf.name;
                token.value = csrf.value;
                form.appendChild(token);
            }
            document.body.appendChild(form);
            form.submit();
        });
    });

    function updateVisibleCounter() {
        var count = 0;
        for (var i = 1; i <= 10; i++) {
            var existingCard = document.getElementById("existing_card_" + i);
            var slotCard = document.getElementById("slot_card_" + i);
            if (existingCard) {
                count++;
            } else if (slotCard && !slotCard.classList.contains("d-none")) {
                count++;
            }
        }
        if (counterText) counterText.innerText = count;
        if (addMoreBtn) {
            if (count >= 10) {
                addMoreBtn.classList.add("d-none");
            } else {
                addMoreBtn.classList.remove("d-none");
            }
        }
    }

    // Initialize slot counter and button visibility based on active DOM cards
    updateVisibleCounter();
}

/**
 * Client-side Date Boundary Guards
 */
function initDateGuards() {
    var today = new Date().toISOString().split("T")[0];
    var approvalDate = document.getElementById("approvalDate");
    var inventoryAsOn = document.getElementById("inventoryAsOnDate");
    var validUpto = document.getElementById("validUpto");
    var startDate = document.getElementById("startDate");
    var completionDate = document.getElementById("completionDate");

    if (approvalDate) {
        approvalDate.max = today;
        approvalDate.addEventListener("change", function () {
            if (validUpto && approvalDate.value) {
                validUpto.min = approvalDate.value;
            }
        });
    }

    if (inventoryAsOn) {
        inventoryAsOn.max = today;
    }

    if (validUpto && approvalDate && approvalDate.value) {
        validUpto.min = approvalDate.value;
    }

    if (startDate) {
        startDate.addEventListener("change", function () {
            if (completionDate && startDate.value) {
                completionDate.min = startDate.value;
            }
        });
    }

    if (completionDate && startDate && startDate.value) {
        completionDate.min = startDate.value;
    }
}

/**
 * Tomcat refuses a multipart request with more than 50 parts, and this form has about 45 text fields plus up to 10 photo
 * inputs. So when the browser builds the data to send, all text fields are packed into ONE urlencoded value ("__packed")
 * and only the photos that were chosen stay as file parts. The server (PackedFormFilter) unpacks it again. The form on the
 * screen is not touched; only the outgoing data changes.
 */
function packTextFields(formData) {
    var packed = new URLSearchParams();
    Array.from(formData.entries()).forEach(function (entry) {
        var name = entry[0], value = entry[1];
        if (name === "_csrf" || name === "action") return;          // the CSRF check and the button stay ordinary parts
        if (value instanceof File) {
            if (value.size === 0 && !value.name) formData.delete(name);   // an empty photo slot is not worth a part
            return;
        }
        packed.append(name, value);
        formData.delete(name);
    });
    formData.append("__packed", packed.toString());
}
