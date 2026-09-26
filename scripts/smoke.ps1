param([string]$BaseUrl = 'http://127.0.0.1:8080')
$ErrorActionPreference = 'Stop'

function Call([string]$Method, [string]$Path, [string]$Token, [object]$Body, [hashtable]$ExtraHeaders) {
    $headers = @{ Accept = 'application/json' }
    if ($Token) { $headers.Authorization = "Bearer $Token" }
    if ($ExtraHeaders) {
        foreach ($key in $ExtraHeaders.Keys) { $headers[$key] = $ExtraHeaders[$key] }
    }
    $args = @{ Method = $Method; Uri = "$BaseUrl$Path"; Headers = $headers }
    if ($null -ne $Body) {
        $args.ContentType = 'application/json'
        $args.Body = $Body | ConvertTo-Json -Depth 8 -Compress
    }
    Invoke-RestMethod @args
}

$stamp = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$account = "smoke$stamp"
$password = 'ResistanceSmoke2026'
$session = Call POST '/api/v1/auth/register' '' @{ account=$account; email="$account@example.com"; password=$password; displayName='Resistance Smoke' } $null
$token = $session.data.accessToken
$null = Call PUT '/api/v1/resistance/me/profile' $token @{ address='Seoul' } $null
$profile = Call PATCH '/api/v1/resistance/me/profile' $token @{ scenario=2; head=1; body=3; arm=4 } $null
$balances = Call GET '/api/v1/resistance/me/balances' $token $null $null
$inbox = Call GET '/api/v1/notifications' $token $null $null
$rotated = Call POST '/api/v1/auth/refresh' '' @{ refreshToken=$session.data.refreshToken } $null
$logout = Call POST '/api/v1/auth/logout' '' @{ refreshToken=$rotated.data.refreshToken } $null

if ($profile.data.userId -lt 1 -or $profile.data.displayName -ne 'Resistance Smoke' -or $profile.data.scenario -ne 2) {
    throw 'Resistance profile contract smoke assertion failed'
}
if ($null -eq $balances.data.balances -or $balances.data.balances.Count -lt 1) {
    throw 'Resistance balance contract smoke assertion failed'
}
if (-not $logout.data.loggedOut) { throw 'Authentication smoke assertion failed' }
Write-Output "Resistance smoke passed: account=$account notifications=$($inbox.data.items.Count)"
