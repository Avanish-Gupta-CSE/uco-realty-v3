<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>

<c:set var="isEdit" value="${staging != null}" />
<c:set var="pageTitle" value="${changeMode ? 'Change Approved Project' : (isEdit ? 'Edit Housing Project' : 'Create Housing Project')} - UCO Realty" scope="request" />
<c:set var="activeTab" value="projects" scope="request" />
<jsp:include page="common/header.jsp" />

<main class="container py-4" id="main-project-form">
    <div class="d-flex justify-content-between align-items-center mb-3" id="form-top-nav">
        <a href="${pageContext.request.contextPath}/admin/projects" class="btn-back" id="btn-back-project-mgmt">
            &larr; Back to Project Management
        </a>
        <c:if test="${isEdit}">
            <a href="${pageContext.request.contextPath}/admin/projects/staging/<c:out value="${staging.id}"/>/preview" class="btn btn-outline-secondary btn-sm" id="btn-form-preview-project">
                👁️ Preview Project
            </a>
        </c:if>
    </div>

    <div class="card-custom" id="card-form-container">
        <h2 class="card-title-custom" id="form-heading">${changeMode ? 'Change Approved Project' : (isEdit ? 'Edit Housing Project' : 'Add New Housing Project')}</h2>

        <c:if test="${changeMode}">
            <div class="alert alert-info small" id="change-mode-banner">
                <strong><c:out value="${project.projectName}"/></strong> is an <strong>approved</strong> project and stays live, exactly as approved, while you work here.
                Use <em>Save as Draft</em> to keep your edits private; use <em>Send for Approval</em> when ready. A checker then compares your change
                with the approved data and approves, returns or rejects it.
                <c:if test="${staging.status == 'NEEDS_CORRECTION'}">
                    <div class="mt-2 text-warning-emphasis" id="change-returned-note"><strong>Returned by the checker:</strong> <c:out value="${staging.checkerComment}"/></div>
                </c:if>
            </div>
        </c:if>

        <c:if test="${not empty error}">
            <div id="formErrorAlert" class="form-error-alert d-flex align-items-center gap-2">
                <span class="fs-5">⚠️</span>
                <div id="form-error-text"><strong>Submission Error:</strong> <c:out value="${error}"/></div>
            </div>
        </c:if>

        <form action="${pageContext.request.contextPath}/admin/projects/staging/save" method="post" enctype="multipart/form-data" id="projectForm" data-context-path="${pageContext.request.contextPath}">
<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
            <c:if test="${isEdit}">
                <input type="hidden" id="hidden-staging-id" name="stagingId" value="<c:out value="${staging.id}"/>">
            </c:if>

            <!-- 1. Location & Identity -->
            <h3 class="form-section-title" id="section-identity">
                1. Project Identity &amp; Location
            </h3>
            <div class="row g-3">
                <div class="col-md-6 col-lg-4">
                    <label for="projectName" class="form-label fw-semibold small">Project Name *</label>
                    <input type="text" id="projectName" name="projectName" class="form-control" value="<c:out value="${project.projectName}"/>" placeholder="e.g. Royal Palms Residency" required>
                </div>

                <div class="col-md-6 col-lg-4">
                    <label for="zoneName" class="form-label fw-semibold small">Zone Name *</label>
                    <c:choose>
                        <c:when test="${userZone != null && !userZone.isEmpty()}">
                            <input type="text" id="zoneName" name="zoneName" class="form-control bg-light" value="${userZone}" readonly required>
                        </c:when>
                        <c:otherwise>
                            <select id="zoneName" name="zoneName" class="form-select" required>
                                <option value="">-- Select Zone --</option>
                                <c:forEach var="z" items="${zones}">
                                    <option value="${z}" <c:out value="${project.zoneName == z ? 'selected' : ''}"/>>${z}</option>
                                </c:forEach>
                            </select>
                        </c:otherwise>
                    </c:choose>
                </div>

                <div class="col-md-6 col-lg-4">
                    <label for="branchName" class="form-label fw-semibold small">Branch Name *</label>
                    <input type="text" id="branchName" name="branchName" class="form-control" value="<c:out value="${project.branchName}"/>" placeholder="e.g. Kolkata Main Branch" required>
                </div>

                <div class="col-md-6 col-lg-4">
                    <label for="stateName" class="form-label fw-semibold small">State Name *</label>
                    <select id="stateName" name="stateName" class="form-select" required>
                        <option value="">-- Select State --</option>
                        <c:forEach var="st" items="${states}">
                            <option value="${st}" <c:out value="${project.stateName == st ? 'selected' : ''}"/>>${st}</option>
                        </c:forEach>
                    </select>
                </div>

                <div class="col-md-6 col-lg-4">
                    <div class="d-flex justify-content-between align-items-center mb-1">
                        <label for="cityName" class="form-label fw-semibold small mb-0">District / City Name *</label>
                        <button type="button" id="btnToggleCustomCity" class="btn btn-link btn-sm p-0 custom-city-toggle">
                            + Other / Custom City
                        </button>
                    </div>
                    <select id="cityName" name="cityName" class="form-select" data-selected-city="<c:out value="${project.cityName}"/>" required>
                        <option value="">-- Select District / City --</option>
                        <c:if test="${not empty project.cityName}">
                            <option value="<c:out value="${project.cityName}"/>" selected><c:out value="${project.cityName}"/></option>
                        </c:if>
                    </select>

                    <div id="customCityContainer" class="mt-2 d-none">
                        <div class="input-group">
                            <input type="text" id="customCityInput" name="customCity" class="form-control form-control-sm" placeholder="Enter custom district / city name" maxlength="100" autocomplete="off">
                            <button type="button" id="btnCancelCustomCity" class="btn btn-outline-secondary btn-sm">Back to List</button>
                        </div>
                        <div class="form-text text-muted small">Enter the district or city name if not found in the dropdown list.</div>
                    </div>
                </div>

                <div class="col-md-6 col-lg-4">
                    <label for="pinCode" class="form-label fw-semibold small">Pincode *</label>
                    <input type="text" id="pinCode" name="pinCode" class="form-control" value="<c:out value="${project.pinCode}"/>" placeholder="6-digit pincode" pattern="[0-9]{6}" required>
                </div>

                <div class="col-12">
                    <label for="projectAddress" class="form-label fw-semibold small">Project Full Address *</label>
                    <textarea id="projectAddress" name="projectAddress" class="form-control" rows="2" placeholder="Plot No, Street, Landmark..." required><c:out value="${project.projectAddress}"/></textarea>
                </div>

                <div class="col-md-6 col-lg-4">
                    <label for="reraNo" class="form-label fw-semibold small">RERA Registration No. *</label>
                    <input type="text" id="reraNo" name="reraNo" class="form-control" value="<c:out value="${project.reraNo}"/>" placeholder="e.g. WBRERA/P/KOL/2023/000123" required>
                </div>

                <div class="col-md-6 col-lg-4">
                    <label for="propertyId" class="form-label fw-semibold small">Property ID / Bank Ref No.</label>
                    <input type="text" id="propertyId" name="propertyId" class="form-control" value="<c:out value="${project.propertyId}"/>" placeholder="e.g. UCO-PROP-009">
                </div>

                <div class="col-md-6 col-lg-4">
                    <label for="typeOfUnit" class="form-label fw-semibold small">Type of Units</label>
                    <input type="text" id="typeOfUnit" name="typeOfUnit" class="form-control" value="<c:out value="${project.typeOfUnit}"/>" placeholder="e.g. 2BHK, 3BHK, Penthouse">
                </div>
            </div>

            <!-- 2. Validity & Milestone Dates (Date Pickers) -->
            <h3 class="form-section-title" id="section-dates">
                2. Project Validity &amp; Approval Dates
            </h3>
            <div class="row g-3">
                <div class="col-md-6 col-lg-4">
                    <label for="approvalDate" class="form-label fw-semibold small">Bank Approval Date</label>
                    <input type="date" id="approvalDate" name="approvalDate" class="form-control" value="${project.approvalDate}">
                </div>

                <div class="col-md-6 col-lg-4">
                    <label for="validUpto" class="form-label fw-semibold small">Approval Valid Upto Date</label>
                    <input type="date" id="validUpto" name="validUpto" class="form-control" value="${project.validUpto}">
                </div>

                <div class="col-md-6 col-lg-4">
                    <label for="startDate" class="form-label fw-semibold small">Project Start Date</label>
                    <input type="date" id="startDate" name="startDate" class="form-control" value="${project.startDate}">
                </div>

                <div class="col-md-6 col-lg-4">
                    <label for="completionDate" class="form-label fw-semibold small">Project Completion Date</label>
                    <input type="date" id="completionDate" name="completionDate" class="form-control" value="${project.completionDate}">
                </div>

                <div class="col-md-6 col-lg-4">
                    <label for="inventoryAsOnDate" class="form-label fw-semibold small">Inventory As On Date</label>
                    <input type="date" id="inventoryAsOnDate" name="inventoryAsOnDate" class="form-control" value="${project.inventoryAsOnDate}">
                </div>

                <div class="col-md-6 col-lg-4">
                    <label for="renewalPending" class="form-label fw-semibold small">Renewal Pending</label>
                    <select id="renewalPending" name="renewalPending" class="form-select">
                        <option value="">-- Select Status --</option>
                        <option value="No" <c:out value="${project.renewalPending == 'No' ? 'selected' : ''}"/>>No</option>
                        <option value="Yes" <c:out value="${project.renewalPending == 'Yes' ? 'selected' : ''}"/>>Yes</option>
                    </select>
                </div>

                <div class="col-md-6 col-lg-4">
                    <label for="approvalSought" class="form-label fw-semibold small">Approval Sought</label>
                    <input type="text" id="approvalSought" name="approvalSought" class="form-control" value="<c:out value="${project.approvalSought}"/>" placeholder="e.g. In-Principle / Full Approval">
                </div>
            </div>

            <!-- 3. Inventory & Pricing -->
            <h3 class="form-section-title" id="section-inventory">
                3. Inventory &amp; Pricing
            </h3>
            <div class="row g-3">
                <div class="col-md-6 col-lg-4">
                    <label for="totalInventory" class="form-label fw-semibold small">Total Inventory (Total Units) *</label>
                    <input type="number" id="totalInventory" name="totalInventory" class="form-control" value="${project.totalInventory}" min="1" required>
                </div>

                <div class="col-md-6 col-lg-4">
                    <label for="unsoldInventory" class="form-label fw-semibold small">Available / Unsold Inventory</label>
                    <input type="number" id="unsoldInventory" name="unsoldInventory" class="form-control" value="${project.unsoldInventory}" min="0">
                </div>

                <div class="col-md-6 col-lg-4">
                    <label for="budgetMin" class="form-label fw-semibold small">Budget Min</label>
                    <input type="text" id="budgetMin" name="budgetMin" class="form-control" value="<c:out value="${project.budgetMin}"/>" placeholder="e.g. 45 Lakhs">
                </div>

                <div class="col-md-6 col-lg-4">
                    <label for="budgetMax" class="form-label fw-semibold small">Budget Max</label>
                    <input type="text" id="budgetMax" name="budgetMax" class="form-control" value="<c:out value="${project.budgetMax}"/>" placeholder="e.g. 1.25 Crore">
                </div>

                <div class="col-md-6 col-lg-4">
                    <label for="avgRate" class="form-label fw-semibold small">Average Rate (per sq.ft.)</label>
                    <input type="text" id="avgRate" name="avgRate" class="form-control" value="<c:out value="${project.avgRate}"/>" placeholder="e.g. 6,500/sq.ft.">
                </div>

                <div class="col-md-6 col-lg-4">
                    <label for="totalProjectArea" class="form-label fw-semibold small">Total Project Area</label>
                    <input type="text" id="totalProjectArea" name="totalProjectArea" class="form-control" value="<c:out value="${project.totalProjectArea}"/>" placeholder="e.g. 5.5 Acres">
                </div>

                <div class="col-md-6 col-lg-4">
                    <label for="carpetMin" class="form-label fw-semibold small">Carpet Area Min (sq.ft.)</label>
                    <input type="text" id="carpetMin" name="carpetMin" class="form-control" value="<c:out value="${project.carpetMin}"/>" placeholder="e.g. 650">
                </div>

                <div class="col-md-6 col-lg-4">
                    <label for="carpetMax" class="form-label fw-semibold small">Carpet Area Max (sq.ft.)</label>
                    <input type="text" id="carpetMax" name="carpetMax" class="form-control" value="<c:out value="${project.carpetMax}"/>" placeholder="e.g. 1450">
                </div>

                <div class="col-md-6 col-lg-4">
                    <label for="buildupMin" class="form-label fw-semibold small">Super Buildup Min (sq.ft.)</label>
                    <input type="text" id="buildupMin" name="buildupMin" class="form-control" value="<c:out value="${project.buildupMin}"/>" placeholder="e.g. 850">
                </div>

                <div class="col-md-6 col-lg-4">
                    <label for="buildupMax" class="form-label fw-semibold small">Super Buildup Max (sq.ft.)</label>
                    <input type="text" id="buildupMax" name="buildupMax" class="form-control" value="<c:out value="${project.buildupMax}"/>" placeholder="e.g. 1850">
                </div>

                <div class="col-md-6 col-lg-4">
                    <label for="noOfTowers" class="form-label fw-semibold small">Number of Towers</label>
                    <input type="number" id="noOfTowers" name="noOfTowers" class="form-control" value="${project.noOfTowers}" min="0">
                </div>

                <div class="col-md-6 col-lg-4">
                    <label for="aptPerFloor" class="form-label fw-semibold small">Apartments Per Floor</label>
                    <input type="number" id="aptPerFloor" name="aptPerFloor" class="form-control" value="${project.aptPerFloor}" min="0">
                </div>

                <div class="col-md-6 col-lg-4">
                    <label for="distRailway" class="form-label fw-semibold small">Distance to Railway Station</label>
                    <input type="text" id="distRailway" name="distRailway" class="form-control" value="<c:out value="${project.distRailway}"/>" placeholder="e.g. 2.5 km">
                </div>

                <div class="col-md-6 col-lg-4">
                    <label for="distAirport" class="form-label fw-semibold small">Distance to Airport</label>
                    <input type="text" id="distAirport" name="distAirport" class="form-control" value="<c:out value="${project.distAirport}"/>" placeholder="e.g. 12 km">
                </div>

                <div class="col-12">
                    <label for="amenities" class="form-label fw-semibold small">Amenities</label>
                    <textarea id="amenities" name="amenities" class="form-control" rows="2" placeholder="e.g. Swimming Pool, Gymnasium, Clubhouse, 24x7 Power Backup, Covered Car Parking..."><c:out value="${project.amenities}"/></textarea>
                </div>
            </div>

            <!-- 4. Builder & Promoters Information -->
            <h3 class="form-section-title" id="section-builder">
                4. Builder &amp; Promoters Information
            </h3>
            <div class="row g-3">
                <div class="col-md-6 col-lg-4">
                    <label for="builderName" class="form-label fw-semibold small">Builder Name *</label>
                    <input type="text" id="builderName" name="builderName" class="form-control" value="<c:out value="${project.builderName}"/>" placeholder="e.g. Meru Developers Pvt Ltd" required>
                </div>

                <div class="col-md-6 col-lg-4">
                    <label for="website" class="form-label fw-semibold small">Builder Website</label>
                    <input type="url" id="website" name="website" class="form-control" value="<c:out value="${project.website}"/>" placeholder="https://www.builder.com">
                </div>

                <div class="col-md-6 col-lg-4">
                    <label for="builderRating" class="form-label fw-semibold small">Builder Rating</label>
                    <input type="text" id="builderRating" name="builderRating" class="form-control" value="<c:out value="${project.builderRating}"/>" placeholder="e.g. CRISIL DA2+">
                </div>

                <div class="col-12">
                    <label for="builderBg" class="form-label fw-semibold small">Builder Background &amp; Track Record</label>
                    <textarea id="builderBg" name="builderBg" class="form-control" rows="2" placeholder="Promoter history, completed projects, years in operation..."><c:out value="${project.builderBg}"/></textarea>
                </div>
            </div>

            <!-- 5. Sales & Bank Contact Personnel -->
            <h3 class="form-section-title" id="section-contacts">
                5. Sales &amp; Bank Contact Personnel
            </h3>
            <div class="row g-3">
                <div class="col-md-6 col-lg-4">
                    <label for="salesExecName" class="form-label fw-semibold small">Sales Executive Name</label>
                    <input type="text" id="salesExecName" name="salesExecName" class="form-control" value="<c:out value="${project.salesExecName}"/>" placeholder="Executive Name">
                </div>

                <div class="col-md-6 col-lg-4">
                    <label for="salesMobile" class="form-label fw-semibold small">Sales Executive Mobile</label>
                    <input type="tel" id="salesMobile" name="salesMobile" class="form-control" value="<c:out value="${project.salesMobile}"/>" placeholder="10-digit mobile">
                </div>

                <div class="col-md-6 col-lg-4">
                    <label for="salesEmail" class="form-label fw-semibold small">Sales Executive Email</label>
                    <input type="email" id="salesEmail" name="salesEmail" class="form-control" value="<c:out value="${project.salesEmail}"/>" placeholder="sales@builder.com">
                </div>

                <div class="col-md-6 col-lg-4">
                    <label for="branchHeadName" class="form-label fw-semibold small">Branch Head Name</label>
                    <input type="text" id="branchHeadName" name="branchHeadName" class="form-control" value="<c:out value="${project.branchHeadName}"/>" placeholder="Name of Chief Manager / Branch Head">
                </div>

                <div class="col-md-6 col-lg-4">
                    <label for="branchHeadNumber" class="form-label fw-semibold small">Branch Head Contact</label>
                    <input type="text" id="branchHeadNumber" name="branchHeadNumber" class="form-control" value="<c:out value="${project.branchHeadNumber}"/>" placeholder="Phone or Mobile">
                </div>

                <div class="col-md-6 col-lg-4">
                    <label for="relExecName" class="form-label fw-semibold small">Relationship Executive Name</label>
                    <input type="text" id="relExecName" name="relExecName" class="form-control" value="<c:out value="${project.relExecName}"/>" placeholder="Home Loan Executive Name">
                </div>

                <div class="col-md-6 col-lg-4">
                    <label for="relExecNumber" class="form-label fw-semibold small">Relationship Executive Contact</label>
                    <input type="text" id="relExecNumber" name="relExecNumber" class="form-control" value="<c:out value="${project.relExecNumber}"/>" placeholder="Phone or Mobile">
                </div>
            </div>

            <!-- 6. Project Photographs (10 Dedicated Upload Slots) -->
            <h3 class="form-section-title" id="section-photos">
                6. Project Photographs (Upload up to 10 Photos — Max 2MB each)
            </h3>
            <p class="text-muted small mb-3" id="photos-hint-text">
                Upload distinct project photos into the separate slots below (JPG, PNG, WEBP). Showing 4 slots initially. Click <strong>"+ Add More Photos"</strong> to add more slots (up to 10 photos total). Maximum 2MB per photo.
            </p>

            <c:set var="existingCount" value="${not empty existingCount ? existingCount : (not empty existingImages ? existingImages.size() : 0)}"/>
            <c:set var="initialVisible" value="${not empty initialVisible ? initialVisible : (existingCount > 4 ? existingCount : 4)}"/>

            <div class="photo-slots-grid" id="photo-slots-grid">
                <c:forEach var="i" begin="1" end="10">
                    <c:choose>
                        <%-- Slot has an existing photo from the database --%>
                        <c:when test="${i <= existingCount}">
                            <c:set var="curImg" value="${existingImages[i - 1]}"/>
                            <div class="photo-slot-card existing-slot justify-content-between" id="existing_card_${i}">
                                <div>
                                    <div class="d-flex justify-content-between align-items-center mb-2">
                                        <span class="fw-bold small text-primary" id="existing-slot-title-${i}">Photo ${i}</span>
                                        <span class="badge badge-approved" id="existing-slot-badge-${i}">Saved</span>
                                    </div>
                                    <img src="${pageContext.request.contextPath}/files/<c:out value="${curImg.id}"/>" alt="Photo ${i}" class="slot-preview-img" id="existing-slot-img-${i}">
                                    <div class="small text-muted mt-1 text-truncate" id="existing-slot-name-${i}" title="<c:out value="${curImg.fileName}"/>">
                                        <c:out value="${curImg.fileName}"/>
                                    </div>
                                </div>
                                <div class="mt-2">
                                    <button type="button" class="slot-delete-btn slot-delete-saved-btn"
                                            id="btn-delete-saved-photo-${curImg.id}"
                                            data-image-id="${curImg.id}"
                                            data-project-id="${staging.projectId}"
                                            data-delete-url="${pageContext.request.contextPath}/admin/projects/staging/<c:out value="${staging.id}"/>/photos/<c:out value="${curImg.id}"/>/remove"
                                            data-context-path="${pageContext.request.contextPath}">
                                        ✕ ${changeMode ? 'Remove Photo' : 'Delete Photo'}
                                    </button>
                                </div>
                            </div>
                        </c:when>

                        <%-- Slot is empty, ready for upload with dedicated icon --%>
                        <c:otherwise>
                            <div class="photo-slot-card upload-slot position-relative ${i <= initialVisible ? 'd-flex' : 'd-none'}" id="slot_card_${i}">
                                <c:if test="${i > 4}">
                                    <button type="button" class="slot-remove-extra-btn" id="btn-remove-slot-${i}" data-slot="${i}" title="Remove this slot">✕</button>
                                </c:if>

                                <input type="file" id="photo_input_${i}" name="photo${i}" accept="image/png,image/jpeg,image/webp" class="d-none slot-file-input" data-slot="${i}">

                                <div id="upload_prompt_${i}" class="slot-upload-prompt" data-slot="${i}">
                                    <div class="slot-camera-icon-wrap" id="slot-camera-wrap-${i}">
                                        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                            <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
                                            <circle cx="8.5" cy="8.5" r="1.5"/>
                                            <polyline points="21 15 16 10 5 21"/>
                                            <line x1="12" y1="9" x2="12" y2="15"/>
                                            <line x1="9" y1="12" x2="15" y2="12"/>
                                        </svg>
                                    </div>
                                    <strong class="text-primary small mb-1" id="slot-upload-title-${i}">Upload Photo ${i}</strong>
                                    <span class="text-info small fw-semibold" id="slot-select-hint-${i}">Click to select photo</span>
                                    <span class="text-muted extra-small" id="slot-rules-hint-${i}">JPG, PNG, WEBP (Max 2MB)</span>
                                </div>

                                <div id="preview_wrap_${i}" class="slot-preview-wrap d-none">
                                    <div>
                                        <div class="d-flex justify-content-between align-items-center mb-2">
                                            <span class="fw-bold small text-primary" id="preview-slot-title-${i}">Photo ${i} (New)</span>
                                            <span id="preview_size_${i}" class="badge bg-light text-primary border"></span>
                                        </div>
                                        <img id="preview_img_${i}" src="" alt="Preview ${i}" class="slot-preview-img">
                                        <div id="preview_name_${i}" class="small text-muted mt-1 text-truncate"></div>
                                    </div>
                                    <div class="mt-2">
                                        <button type="button" class="slot-clear-btn" id="btn-clear-slot-${i}" data-slot="${i}">
                                            ✕ Change / Remove
                                        </button>
                                    </div>
                                </div>
                            </div>
                        </c:otherwise>
                    </c:choose>
                </c:forEach>
            </div>

            <!-- Dynamic Slot Controller (+ Add More Photos) -->
            <div class="d-flex align-items-center justify-content-between p-3 bg-light border rounded mb-4 flex-wrap gap-2" id="slot-controller-bar">
                <div class="d-flex align-items-center gap-3" id="add-more-photos-wrap">
                    <button type="button" id="addMorePhotosBtn" class="add-photos-btn ${initialVisible >= 10 ? 'd-none' : ''}">
                        <span class="fs-6 fw-bold">+</span> Add More Photos (Up to 10)
                    </button>
                    <small class="text-muted" id="add-photos-help-text">Add extra photo slots beyond initial 4 (max 10 total)</small>
                </div>
                <div class="slot-counter-badge" id="slotCounterBadge">
                    <span id="visibleSlotCountText">${initialVisible}</span> / 10 Photo Slots Active
                </div>
            </div>

            <!-- Submission Actions -->
            <div class="d-flex gap-2 mt-4 pt-3 border-top flex-wrap" id="form-action-buttons">
                <button type="submit" name="action" value="draft" formnovalidate class="btn btn-secondary px-4" id="btn-save-draft">
                    ${changeMode ? 'Save as Draft' : 'Save Draft'}
                </button>
                <button type="submit" name="action" value="submit" class="btn btn-success px-4" id="btn-save-submit" data-confirm="${changeMode ? 'Send this change to the checker? The approved project stays live until it is approved.' : 'Submit this project for Checker approval?'}">
                    ${changeMode ? 'Send for Approval' : 'Save &amp; Request Approval'}
                </button>
                <a href="${pageContext.request.contextPath}/admin/projects" class="btn btn-outline-secondary ms-auto" id="btn-cancel-form">
                    Cancel
                </a>
            </div>
        </form>
    </div>
</main>

<!-- Project Form Script -->
<script id="project-form-js" src="${pageContext.request.contextPath}/assets/js/project-form.js?v=${assetVersion}"></script>

<jsp:include page="common/footer.jsp"/>
