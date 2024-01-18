param (
    [Parameter(Mandatory=$true)] $ImageName
)
Write-Host "Checking $ImageName"
docker image inspect $ImageName | out-null
if ($LastExitCode -ne 0){
    exit
}
$RunningContainers = docker ps --no-trunc --filter "ancestor=$ImageName" --format "{{.Names}}"
if ($RunningContainers.length -gt 0)
{
    Write-Host "No cleaning. Still running containers: $RunningContainers"
    exit
}
else {
    docker image rm --no-prune $ImageName
    Write-Host "Done"
}
