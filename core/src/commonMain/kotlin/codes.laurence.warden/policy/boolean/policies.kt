package codes.laurence.warden.policy.boolean

import codes.laurence.warden.Access
import codes.laurence.warden.AccessRequest
import codes.laurence.warden.AccessResponse
import codes.laurence.warden.policy.Policy
import codes.laurence.warden.policy.PolicyDSL
import codes.laurence.warden.policy.collections.CollectionBasedPolicy

/**
 * All of the policies must grant access, and at least 1 must grant access.
 */
@PolicyDSL
class AllOf(
    policies: List<Policy>,
    override val id: String? = null,
) : Policy, CollectionBasedPolicy(policies.toMutableList()) {

    constructor(id: String? = null, builder: CollectionBasedPolicy.() -> Unit) : this(mutableListOf(), id) {
        this.builder()
    }

    constructor(vararg policies: Policy, id: String? = null) : this(policies.toMutableList(), id)

    override fun checkAuthorized(accessRequest: AccessRequest): AccessResponse {
        if (policies.isEmpty()) {
            return AccessResponse(Access.Denied(), accessRequest)
        } else {
            policies.forEach {
                val response = it.checkAuthorized(accessRequest)
                when (response.access) {
                    is Access.Denied -> {
                        return response
                    }
                    else -> {
                    }
                }
            }
            return AccessResponse(Access.Granted(), accessRequest)
        }
    }
}

/**
 * At least 1 policy must grant access.
 */
@PolicyDSL
class AnyOf(
    policies: List<Policy>,
    override val id: String? = null
) : Policy, CollectionBasedPolicy(policies.toMutableList()) {

    constructor(id: String? = null, builder: CollectionBasedPolicy.() -> Unit) : this(mutableListOf(), id) {
        this.builder()
    }

    constructor(vararg policies: Policy, id: String? = null) : this(policies.toMutableList(), id)

    override fun checkAuthorized(accessRequest: AccessRequest): AccessResponse {
        policies.forEach {
            val response = it.checkAuthorized(accessRequest)
            when (response.access) {
                is Access.Granted -> {
                    return response
                }
                else -> {
                }
            }
        }
        return AccessResponse(
            access = Access.Denied(),
            request = accessRequest
        )
    }
}

/**
 * Will grant access if the policy does not grant access.
 */
@PolicyDSL
class Not(val policy: Policy, override val id: String? = null) : Policy {

    override fun checkAuthorized(accessRequest: AccessRequest): AccessResponse {
        val internal = policy.checkAuthorized(accessRequest)
        return when (internal.access) {
            is Access.Granted -> {
                internal.copy(access = Access.Denied())
            }
            is Access.Denied -> {
                internal.copy(access = Access.Granted())
            }
        }
    }
}
