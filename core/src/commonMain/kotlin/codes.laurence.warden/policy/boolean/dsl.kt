package codes.laurence.warden.policy.boolean

import codes.laurence.warden.policy.Policy
import codes.laurence.warden.policy.collections.CollectionBasedPolicy

/**
 * The equivalent of an AND statement for each policy in the collection. Empty collections always evaluate to false.
 */
fun allOf(id: String? = null, builder: CollectionBasedPolicy.() -> Unit) = AllOf(id, builder)


/**
 * The equivalent of an OR statement for each policy in the collection. Empty collections always evaluate to false.
 */
fun anyOf(id: String? = null, builder: CollectionBasedPolicy.() -> Unit) = AnyOf(id, builder)

/**
 * Negation for the wrapped policy
 */
fun not(id: String? = null, policy: Policy): Policy = Not(policy, id)
