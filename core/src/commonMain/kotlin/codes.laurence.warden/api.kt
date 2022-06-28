package codes.laurence.warden

data class AccessRequest(
    val subject: Map<String, Any?> = emptyMap(),
    val action: Map<String, Any?> = emptyMap(),
    val resource: Map<String, Any?> = emptyMap(),
    val environment: Map<String, Any?> = emptyMap()
)

data class AccessResponse(
    val access: Access,
    val request: AccessRequest,
    val trace: AccessEvaluationTrace? = null
)

data class AccessRequestBatch(
    val subject: Map<String, Any?> = emptyMap(),
    val action: Map<String, Any?> = emptyMap(),
    val resources: List<Map<String, Any?>> = emptyList(),
    val environment: Map<String, Any?> = emptyMap()
)

data class FilterAccessRequest<RESOURCE>(
    val subject: Map<String, Any?> = emptyMap(),
    val action: Map<String, Any?> = emptyMap(),
    val resources: List<ResourceAttributePair<RESOURCE>> = emptyList(),
    val environment: Map<String, Any?> = emptyMap()
)

data class ResourceAttributePair<RESOURCE>(
    val resource: RESOURCE,
    val attributes: Map<String, Any?>
)

data class AccessEvaluationTrace(
    val policyDescription: String,
    val access: Access,
    val note: String?= null,
    val children: List<AccessEvaluationTrace> = emptyList(),
)

sealed class Access {
    data class Granted(
        val properties: Map<String, Any?> = emptyMap()
    ) : Access()

    data class Denied(
        val properties: Map<String, Any?> = emptyMap(),
    ) : Access()
}
