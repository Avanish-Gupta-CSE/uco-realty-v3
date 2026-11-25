/**
 * Project form: field checks while the maker types, with the message right under the field (red border).
 * The rules are the same as the server's ProjectValidator (the server still checks everything again when the form is saved).
 * "Required" messages appear when the maker leaves a field empty, or when Submit for Approval is pressed; "Save as Draft" only
 * stops for wrongly typed values, not for empty fields.
 * No inline script or style (bank CSP): this file and the .field-error rule in style.css.
 * Change this file when: a field rule in service/ProjectValidator.java changes (keep the two in step).
 */
(function () {
    "use strict";

    var form = document.getElementById("projectForm");
    if (!form) return;
    form.noValidate = true;   // our messages replace the browser's pop-up, which only appears when the form is sent

    // Longest text each field may hold (same numbers as ProjectValidator.MAX); typing stops at the limit
    var MAX = {
        branchName: 100, propertyId: 100, typeOfUnit: 100, totalProjectArea: 100, distRailway: 100, distAirport: 100, avgRate: 100,
        budgetMin: 100, budgetMax: 100, approvalSought: 100, reraNo: 100, carpetMin: 50, carpetMax: 50, buildupMin: 50, buildupMax: 50,
        builderRating: 50, builderName: 255, salesExecName: 255, branchHeadName: 255, relExecName: 255, salesEmail: 255, website: 255,
        salesMobile: 15, branchHeadNumber: 15, relExecNumber: 15, projectName: 255, projectAddress: 500, pinCode: 10,
        builderRatingOutOf: 20, videoUrl: 500, amenities: 4000, builderBg: 4000
    };

    var RERA = /^[A-Za-z0-9][A-Za-z0-9 \/._-]{2,38}[A-Za-z0-9]$/;
    var PHONE_CHARS = /^[0-9+()\- ]+$/;
    var EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;
    var PIN = /^[1-9][0-9]{5}$/;
    var LEADING_NUMBER = /^\s*[₹]?\s*([0-9][0-9,]*(?:\.[0-9]+)?)\s*([A-Za-z]*)/;

    function el(id) { return document.getElementById(id); }
    function val(id) { var e = el(id); return e ? String(e.value).trim() : ""; }
    function digitCount(v) { return (v.match(/[0-9]/g) || []).length; }
    function today() {
        var d = new Date();
        return d.getFullYear() + "-" + ("0" + (d.getMonth() + 1)).slice(-2) + "-" + ("0" + d.getDate()).slice(-2);
    }
    function validUrl(u) {
        try {
            var x = new URL(u);
            return (x.protocol === "http:" || x.protocol === "https:") && x.hostname.indexOf(".") >= 0;
        } catch (e) { return false; }
    }
    // number in rupees for "38.0 L", "1.20 Cr"; plain number otherwise (money false); null when there is no number
    function amount(text, money) {
        var m = LEADING_NUMBER.exec(text || "");
        if (!m) return null;
        var n = parseFloat(m[1].replace(/,/g, ""));
        if (!money) return n;
        var u = m[2].toLowerCase();
        if (u.indexOf("cr") === 0) return n * 10000000;
        if (u === "l" || u.indexOf("lac") === 0 || u.indexOf("lak") === 0) return n * 100000;
        if (u === "k") return n * 1000;
        return n;
    }

    // One function per field: gets the typed value, returns a short message or "" when the value is fine (empty values are fine here)
    var RULES = {};
    RULES.projectName = function (v) { return v && v.length < 3 ? "Project name must be at least 3 characters" : ""; };
    RULES.reraNo = function (v) {
        return v && v.toLowerCase() !== "not applicable" && !RERA.test(v)
            ? "Use letters, digits, spaces and / . _ - (4 to 40 characters), or write \"Not applicable\"" : "";
    };
    RULES.pinCode = function (v) { return v && !PIN.test(v) ? "Pincode must be 6 digits and cannot start with 0" : ""; };
    function phoneRule(v) {
        if (!v) return "";
        var d = digitCount(v);
        return !PHONE_CHARS.test(v) || d < 8 || d > 13 ? "Enter a phone number of 8 to 13 digits (you may use + ( ) - and spaces)" : "";
    }
    RULES.salesMobile = phoneRule;
    RULES.branchHeadNumber = phoneRule;
    RULES.relExecNumber = phoneRule;
    RULES.salesEmail = function (v) { return v && !EMAIL.test(v) ? "This is not a valid email address" : ""; };
    function urlRule(v) { return v && !validUrl(v) ? "Enter a full address starting with http:// or https://" : ""; }
    RULES.website = urlRule;
    RULES.videoUrl = urlRule;
    function intRule(min, max, text) {
        return function (v) {
            if (v === "") return "";
            var n = Number(v);
            return isNaN(n) || n < min || n > max || Math.floor(n) !== n ? text : "";
        };
    }
    RULES.totalInventory = intRule(1, 100000, "Total inventory must be a whole number from 1 to 100000");
    RULES.aptPerFloor = intRule(1, 100, "Apartments per floor must be from 1 to 100");
    RULES.noOfTowers = intRule(1, 500, "Number of towers must be from 1 to 500");
    RULES.unsoldInventory = function (v) {
        if (v === "") return "";
        var n = Number(v);
        if (isNaN(n) || n < 0 || Math.floor(n) !== n) return "Available inventory must be a whole number, 0 or more";
        var total = val("totalInventory");
        return total !== "" && n > Number(total) ? "Available inventory cannot exceed total inventory" : "";
    };
    function digitRule(label) { return function (v) { return v && !/[0-9]/.test(v) ? label + " must include a number" : ""; }; }
    RULES.totalProjectArea = digitRule("Total project area");
    RULES.distRailway = digitRule("Distance to railway station");
    RULES.distAirport = digitRule("Distance to airport");
    RULES.avgRate = digitRule("Average rate");
    function rangeRule(minId, maxId, money, label) {
        return function (v, id) {
            if (!v) return "";
            if (amount(v, money) === null) return label + (id === minId ? " minimum" : " maximum") + " must start with a number";
            var a = amount(val(minId), money), b = amount(val(maxId), money);
            return a !== null && b !== null && a > b ? label + " minimum cannot be more than the maximum" : "";
        };
    }
    RULES.carpetMin = RULES.carpetMax = rangeRule("carpetMin", "carpetMax", false, "Carpet area");
    RULES.buildupMin = RULES.buildupMax = rangeRule("buildupMin", "buildupMax", false, "Built-up area");
    RULES.budgetMin = RULES.budgetMax = rangeRule("budgetMin", "budgetMax", true, "Budget");
    function dateRule(notInFuture, label) {
        return function (v) {
            if (!v) return "";
            if (v < "2000-01-01" || v > "2100-12-31") return label + " is not a sensible date";
            return notInFuture && v > today() ? label + " cannot be in the future" : "";
        };
    }
    RULES.approvalDate = function (v) {
        var m = dateRule(true, "Approval date")(v);
        return m;
    };
    RULES.inventoryAsOnDate = dateRule(true, "Inventory as-on date");
    RULES.startDate = dateRule(false, "Project start date");
    RULES.completionDate = function (v) {
        var m = dateRule(false, "Completion date")(v);
        if (m) return m;
        var s = val("startDate");
        return v && s && v < s ? "Completion date cannot be before the start date" : "";
    };
    RULES.validUpto = function (v) {
        var m = dateRule(false, "Valid-upto date")(v);
        if (m) return m;
        var a = val("approvalDate");
        return v && a && v < a ? "Valid-upto date cannot be before the approval date" : "";
    };

    // when one field changes, these are checked again (they depend on it)
    var DEPENDENTS = {
        totalInventory: ["unsoldInventory"], carpetMin: ["carpetMax"], carpetMax: ["carpetMin"], buildupMin: ["buildupMax"],
        buildupMax: ["buildupMin"], budgetMin: ["budgetMax"], budgetMax: ["budgetMin"], startDate: ["completionDate"],
        approvalDate: ["validUpto"]
    };

    var touched = {};   // fields the maker has already left once

    function isShown(e) { return !!e && !e.disabled && e.offsetParent !== null; }
    function isRequired(e) { return e.id === "customCityInput" || e.hasAttribute("required"); }

    function errorBox(e) {
        var box = el("field-error-" + e.id);
        if (!box) {
            box = document.createElement("div");
            box.className = "field-error";
            box.id = "field-error-" + e.id;
            box.setAttribute("role", "alert");
            var anchor = e.parentNode && e.parentNode.classList.contains("input-group") ? e.parentNode : e;
            anchor.parentNode.insertBefore(box, anchor.nextSibling);
        }
        return box;
    }
    function show(e, message) {
        var box = el("field-error-" + e.id);
        if (message) {
            box = errorBox(e);
            box.textContent = message;
            e.classList.add("is-invalid");
            e.setAttribute("aria-invalid", "true");
            e.setAttribute("aria-describedby", box.id);
        } else {
            if (box) box.remove();
            e.classList.remove("is-invalid");
            e.removeAttribute("aria-invalid");
            e.removeAttribute("aria-describedby");
        }
    }

    // the message for one field now ("" = fine); empty required fields only count when `withRequired` is true
    function check(e, withRequired) {
        var v = String(e.value).trim();
        if (v === "") return withRequired && isRequired(e) ? "This field is required" : "";
        if (v.indexOf("<") >= 0 || v.indexOf(">") >= 0) return "The characters < and > are not allowed";
        var rule = RULES[e.id];
        return rule ? rule(v, e.id) : "";
    }

    function validateField(e) {
        if (!isShown(e)) { show(e, ""); return ""; }
        var message = check(e, !!touched[e.id]);
        show(e, message);
        return message;
    }
    function refresh(id) {
        var e = el(id);
        if (!e) return;
        validateField(e);
        (DEPENDENTS[id] || []).forEach(function (other) {
            var o = el(other);
            if (o && (touched[other] || String(o.value).trim() !== "")) validateField(o);
        });
    }

    var ids = {};
    Object.keys(RULES).forEach(function (id) { ids[id] = true; });
    Array.prototype.forEach.call(form.querySelectorAll("input[required], select[required], textarea[required]"), function (e) { if (e.id) ids[e.id] = true; });
    ids.customCityInput = true;

    Object.keys(ids).forEach(function (id) {
        var e = el(id);
        if (!e) return;
        if (MAX[id] && !e.getAttribute("maxlength") && (e.tagName === "TEXTAREA" || e.type === "text")) e.maxLength = MAX[id];
        e.addEventListener("input", function () { refresh(id); });
        e.addEventListener("change", function () { refresh(id); });
        e.addEventListener("blur", function () { touched[id] = true; refresh(id); });
    });
    // other text boxes of the form (promoters, notes ...): only the < > rule
    Array.prototype.forEach.call(form.querySelectorAll("input[type=text], textarea"), function (e) {
        if (!e.id || ids[e.id] || e.name.indexOf("otherTitle_") === 0 || e.name === "__packed") return;
        ids[e.id] = true;
        if (MAX[e.id] && !e.getAttribute("maxlength")) e.maxLength = MAX[e.id];
        e.addEventListener("input", function () { validateField(e); });
    });

    // Submit: check every field, stop at the first problem and show where it is
    form.addEventListener("submit", function (event) {
        var submitter = event.submitter;
        var draft = !!submitter && submitter.value === "draft";
        var first = null;
        Object.keys(ids).forEach(function (id) {
            var e = el(id);
            if (!e) return;
            var message = isShown(e) ? check(e, !draft) : "";
            if (!draft || message) touched[id] = true;
            show(e, message);
            // the first problem on the page (top to bottom), not the first in the order of the rules
            if (message && (!first || (first.compareDocumentPosition(e) & Node.DOCUMENT_POSITION_PRECEDING))) first = e;
        });
        if (first) {
            event.preventDefault();
            event.stopPropagation();
            var notice = el("formClientNotice");
            if (notice) {
                notice.textContent = "Please correct the fields marked in red.";
                notice.classList.remove("d-none");
            }
            first.scrollIntoView({ behavior: "smooth", block: "center" });
            first.focus();
        }
    }, true);
})();
