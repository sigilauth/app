# Challenge approval
challenge-approve-title = Approve Request
challenge-from-server = { $serverName } requests approval
challenge-action-type = Action: { $actionType }
challenge-action-description = { $actionDescription }
challenge-expires = Expires in { $minutes ->
    [one] { $minutes } minute
   *[other] { $minutes } minutes
}
challenge-approve = Approve
challenge-deny = Deny

# Step-up specific
step-up-title = Approve: { $actionDescription }
step-up-for-user = For user: { $userEmail }
step-up-key-name = Key name: { $keyName }

# Success states
challenge-approved = Approved
challenge-approved-message = Your approval has been recorded.
challenge-denied = Denied
challenge-expired = Request Expired
