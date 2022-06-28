package codes.laurence.warden.policy.members

import codes.laurence.warden.Access
import codes.laurence.warden.AccessEvaluationTrace
import codes.laurence.warden.AccessRequest
import codes.laurence.warden.AccessResponse
import codes.laurence.warden.policy.Policy
import codes.laurence.warden.policy.PolicyDSL
import codes.laurence.warden.policy.expression.*
import codes.laurence.warden.trace.policyBasicDescription

interface MemberPolicy {
    fun checkAuthorized(member: Map<*, *>, accessRequest: AccessRequest): AccessResponse
}

@PolicyDSL
class ForAnyMemberPolicy(
    val memberSource: ValueReference,
    val memberPolicies: List<MemberPolicy>,
    override val id: String? = null,
) : Policy {

    init {
        require(memberPolicies.isNotEmpty()) { "Member policies must not be empty" }
    }

    override fun checkAuthorized(accessRequest: AccessRequest): AccessResponse {
        try {
            val members = getMembers(accessRequest)
            members.forEach { member ->
                if (member !is Map<*, *>) {
                    throw InvalidMemberException("Members must be a Map")
                }
                var deniedBy: AccessResponse? = null
                memberPolicies.forEach { policy ->
                    val response = policy.checkAuthorized(member, accessRequest)
                    if (response.access is Access.Denied) {
                        deniedBy = response
                    }
                }
                if (deniedBy == null) {
                    return AccessResponse(
                        access = Access.Granted(),
                        request = accessRequest,
                        trace = AccessEvaluationTrace(
                            policyDescription = policyBasicDescription(this),
                            access = Access.Granted(),
                        )
                    )
                }
            }
            return AccessResponse(
                access = Access.Denied(),
                request = accessRequest,
                trace = AccessEvaluationTrace(
                    policyDescription = policyBasicDescription(this),
                    access = Access.Denied(),
                )
            )
        } catch (e: InvalidMemberException) {
            val access = Access.Denied()
            return AccessResponse(
                access = access,
                request = accessRequest,
                trace = AccessEvaluationTrace(
                    policyDescription = policyBasicDescription(this),
                    access = access,
                    note = "InvalidMember: ${e.message}",
                )
            )
        } catch (e: NoSuchAttributeException) {
            val access = Access.Denied()
            return AccessResponse(
                access = access,
                request = accessRequest,
                trace = AccessEvaluationTrace(
                    policyDescription = policyBasicDescription(this),
                    access = access,
                    note = "NoSuchAttribute: ${e.message}",
                )
            )
        }
    }

    private fun getMembers(accessRequest: AccessRequest): Collection<*> {
        val members = memberSource.get(accessRequest)
        if (members is Collection<*>) {
            return members
        }
        throw InvalidMemberException("Target attribute must be a collection")
    }
}

@PolicyDSL
class ForAllMembersPolicy(
    val memberSource: ValueReference,
    val memberPolicies: List<MemberPolicy>,
    override val id: String? = null,
) : Policy {

    init {
        require(memberPolicies.isNotEmpty()) { "Member policies must not be empty" }
    }

    override fun checkAuthorized(accessRequest: AccessRequest): AccessResponse {
        try {
            val members = getMembers(accessRequest)
            members.forEach { member ->
                if (member !is Map<*, *>) {
                    throw InvalidMemberException("Members must be a Map")
                }
                memberPolicies.forEach { policy ->
                    val response = policy.checkAuthorized(member, accessRequest)
                    if (response.access is Access.Denied) {
                        return response
                    }
                }
            }
            return AccessResponse(
                access = Access.Granted(),
                request = accessRequest
            )
        } catch (e: InvalidMemberException) {
            return AccessResponse(
                access = Access.Denied(),
                request = accessRequest
            )
        } catch (e: NoSuchAttributeException) {
            return AccessResponse(
                access = Access.Denied(),
                request = accessRequest
            )
        }
    }

    private fun getMembers(accessRequest: AccessRequest): Collection<*> {
        val members = memberSource.get(accessRequest)
        if (members is Collection<*>) {
            return members
        }
        throw InvalidMemberException("Target attribute must be a collection")
    }
}

class InvalidMemberException(message: String) : Exception(message)

data class MemberAttributeReference(
    val path: List<String>
) : ValueReference {

    init {
        require(path.isNotEmpty()) { "path cannot be empty" }
    }

    internal var member: Map<*, *>? = null

    override fun get(accessRequest: AccessRequest): Any? {
        return getValueFromAttributes(
            path,
            member ?: throw IllegalStateException("Member must be set before getting")
        )
    }
}

class MemberExpressionPolicy(
    val leftOperand: MemberAttributeReference,
    operatorType: OperatorType,
    rightOperand: ValueReference
) : MemberPolicy {

    internal var internalExpressionPolicy = ExpressionPolicy(
        leftOperand = leftOperand,
        operatorType = operatorType,
        rightOperand = rightOperand
    )

    override fun checkAuthorized(member: Map<*, *>, accessRequest: AccessRequest): AccessResponse {
        leftOperand.member = member
        return internalExpressionPolicy.checkAuthorized(accessRequest)
    }
}
