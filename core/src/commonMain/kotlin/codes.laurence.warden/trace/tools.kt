package codes.laurence.warden.trace

import codes.laurence.warden.policy.Policy

fun policyBasicDescription(policy: Policy) = "${policy::class.simpleName}(name=${policy.name})"
