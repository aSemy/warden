package codes.laurence.warden.policy.boolean

import codes.laurence.warden.policy.Policy
import codes.laurence.warden.policy.collections.CollectionBasedPolicy

/**
 * The equivalent of an AND statement for each policy in the collection. Empty collections always evaluate to false.
 */
fun allOf(name: String? = null, builder: CollectionBasedPolicy.() -> Unit) = AllOf(name, builder)


/**
 * The equivalent of an OR statement for each policy in the collection. Empty collections always evaluate to false.
 */
fun anyOf(name: String? = null, builder: CollectionBasedPolicy.() -> Unit) = AnyOf(name, builder)

/**
 * Negation for the wrapped policy
 */
fun not(name: String? = null, policy: Policy): Policy = Not(policy, name)
