package com.lankaspot.server.constants;

public final class NotificationConstants {

    private NotificationConstants() {
    }

    // -------------------------------------------------------------------------
    // Advertisement Routes
    // -------------------------------------------------------------------------

    public static final String ADVERTISEMENT_ROUTE =
            "/advertiser/advertisements/";

    public static final String ADVERTISEMENTS_ROUTE =
            "/advertiser/advertisements";

    public static final String DASHBOARD_ROUTE =
            "/dashboard";

    public static final String ADVERTISEMENT_AMEND_ROUTE =
            "/dashboard/amend-advertisement";


    // -------------------------------------------------------------------------
    // Advertisement
    // -------------------------------------------------------------------------

    public static final String AD_PURCHASED_SUCCESSFULLY_TITLE =
            "Advertisement Purchased Successfully";

    public static final String AD_PURCHASED_SUCCESSFULLY_DESCRIPTION =
            "Your advertisement has been purchased successfully and is now being processed.";

    public static final String AD_PURCHASED_SUCCESSFULLY_REDIRECT_TO =
            DASHBOARD_ROUTE;


    public static final String AD_SUBMITTED_FOR_REVIEW_TITLE =
            "Advertisement Submitted for Review";

    public static final String AD_SUBMITTED_FOR_REVIEW_DESCRIPTION =
            "Your advertisement has been submitted successfully and is waiting for review.";

    public static final String AD_SUBMITTED_FOR_REVIEW_REDIRECT_TO =
            ADVERTISEMENT_ROUTE;


    public static final String AD_APPROVED_TITLE =
            "Advertisement Approved";

    public static final String AD_APPROVED_DESCRIPTION =
            "Your advertisement has been approved and is now active.";

    public static final String AD_APPROVED_REDIRECT_TO =
            ADVERTISEMENT_ROUTE;


    public static final String AD_REJECTED_TITLE =
            "Advertisement Rejected";

    public static final String AD_REJECTED_DESCRIPTION =
            "Your advertisement has been rejected. Please review the feedback and make the necessary changes.";

    public static final String AD_REJECTED_REDIRECT_TO =
            ADVERTISEMENT_AMEND_ROUTE;


    public static final String AD_CHANGES_REQUESTED_TITLE =
            "Changes Requested";

    public static final String AD_CHANGES_REQUESTED_DESCRIPTION =
            "Changes have been requested for your advertisement. Please review the feedback and update your advertisement.";

    public static final String AD_CHANGES_REQUESTED_REDIRECT_TO =
            ADVERTISEMENT_ROUTE;


    public static final String AD_EXPIRED_TITLE =
            "Advertisement Expired";

    public static final String AD_EXPIRED_DESCRIPTION =
            "Your advertisement has expired. You can renew it to make it active again.";

    public static final String AD_EXPIRED_REDIRECT_TO =
            ADVERTISEMENT_ROUTE;


    public static final String AD_RENEWED_TITLE =
            "Advertisement Renewed";

    public static final String AD_RENEWED_DESCRIPTION =
            "Your advertisement has been renewed successfully.";

    public static final String AD_RENEWED_REDIRECT_TO =
            ADVERTISEMENT_ROUTE;


    public static final String AD_DEACTIVATED_TITLE =
            "Advertisement Deactivated";

    public static final String AD_DEACTIVATED_DESCRIPTION =
            "Your advertisement has been deactivated.";

    public static final String AD_DEACTIVATED_REDIRECT_TO =
            ADVERTISEMENT_ROUTE;


    public static final String AD_ACTIVATED_TITLE =
            "Advertisement Activated";

    public static final String AD_ACTIVATED_DESCRIPTION =
            "Your advertisement has been activated successfully.";

    public static final String AD_ACTIVATED_REDIRECT_TO =
            ADVERTISEMENT_ROUTE;


    // -------------------------------------------------------------------------
    // Payment
    // -------------------------------------------------------------------------

    public static final String PAYMENT_SUCCESSFUL_TITLE =
            "Payment Successful";

    public static final String PAYMENT_SUCCESSFUL_DESCRIPTION =
            "Your payment has been completed successfully.";

    public static final String PAYMENT_SUCCESSFUL_REDIRECT_TO =
            ADVERTISEMENTS_ROUTE;


    public static final String PAYMENT_FAILED_TITLE =
            "Payment Failed";

    public static final String PAYMENT_FAILED_DESCRIPTION =
            "Your payment could not be completed. Please try again.";

    public static final String PAYMENT_FAILED_REDIRECT_TO =
            ADVERTISEMENTS_ROUTE;


    public static final String PAYMENT_PENDING_TITLE =
            "Payment Pending";

    public static final String PAYMENT_PENDING_DESCRIPTION =
            "Your payment is currently being processed.";

    public static final String PAYMENT_PENDING_REDIRECT_TO =
            ADVERTISEMENTS_ROUTE;


    // -------------------------------------------------------------------------
    // Advertisement Amendment
    // -------------------------------------------------------------------------

    public static final String AD_AMENDMENT_SUBMITTED_TITLE =
            "Advertisement Changes Submitted";

    public static final String AD_AMENDMENT_SUBMITTED_DESCRIPTION =
            "Your advertisement changes have been submitted successfully and are waiting for review.";

    public static final String AD_AMENDMENT_SUBMITTED_REDIRECT_TO =
            ADVERTISEMENT_ROUTE;


    public static final String AD_AMENDMENT_APPROVED_TITLE =
            "Advertisement Changes Approved";

    public static final String AD_AMENDMENT_APPROVED_DESCRIPTION =
            "Your advertisement changes have been approved successfully.";

    public static final String AD_AMENDMENT_APPROVED_REDIRECT_TO =
            ADVERTISEMENT_ROUTE;


    public static final String AD_AMENDMENT_REJECTED_TITLE =
            "Advertisement Changes Rejected";

    public static final String AD_AMENDMENT_REJECTED_DESCRIPTION =
            "Your advertisement changes have been rejected. Please review the feedback and make the necessary changes.";

    public static final String AD_AMENDMENT_REJECTED_REDIRECT_TO =
            ADVERTISEMENT_ROUTE;


    // -------------------------------------------------------------------------
    // Account
    // -------------------------------------------------------------------------

    public static final String ACCOUNT_VERIFIED_TITLE =
            "Account Verified";

    public static final String ACCOUNT_VERIFIED_DESCRIPTION =
            "Your account has been verified successfully.";

    public static final String ACCOUNT_VERIFIED_REDIRECT_TO =
            "/advertiser/profile";


    public static final String ACCOUNT_SUSPENDED_TITLE =
            "Account Suspended";

    public static final String ACCOUNT_SUSPENDED_DESCRIPTION =
            "Your account has been suspended. Please contact support for more information.";

    public static final String ACCOUNT_SUSPENDED_REDIRECT_TO =
            "/advertiser/profile";
}