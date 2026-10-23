package info.prorabka.abik.android.presentation.navigation

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val REGISTER_CONFIRM = "register_confirm/{phone}"
    const val FORGOT = "forgot"
    const val FORGOT_CONFIRM = "forgot_confirm/{phone}"
    const val ONBOARDING_PROFILE = "onboarding_profile"
    const val ONBOARDING_TEAM = "onboarding_team"
    const val MAIN = "main"
    const val PAYMENT_FORM = "payment_form?paymentId={paymentId}&userId={userId}"
    const val NOTIFICATIONS = "notifications"
    const val REPORTS = "reports"
    const val MEMBER_DETAILS = "member/{userId}"
    const val MEMBER_FINANCE = "member_finance/{userId}"
    const val CHARGE_CREATE = "charge_create?userId={userId}"
    const val PROFILE_EDIT = "profile_edit"
    const val CHANGE_PHONE = "change_phone"
    const val ADMIN = "admin"
    const val CHARGE_TEMPLATES = "charge_templates"
    const val TEMPLATE_FORM = "template_form?templateId={templateId}"

    fun registerConfirm(phone: String) = "register_confirm/$phone"
    fun forgotConfirm(phone: String) = "forgot_confirm/$phone"
    fun paymentForm(paymentId: Long = -1L, userId: Long = -1L) =
        "payment_form?paymentId=$paymentId&userId=$userId"
    fun memberDetails(userId: Long) = "member/$userId"
    fun memberFinance(userId: Long) = "member_finance/$userId"
    fun chargeCreate(userId: Long = -1L) = "charge_create?userId=$userId"
    fun templateForm(templateId: Long = -1L) = "template_form?templateId=$templateId"
}
