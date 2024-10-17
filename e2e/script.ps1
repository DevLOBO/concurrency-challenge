# Parameters for the script
$iterations = 50000      # Total number of iterations
$concurrency = 50        # Number of concurrent Newman instances
$iterationsPerInstance = [math]::Floor($iterations / $concurrency) # Iterations per Newman instance

# The path to your Postman collection
$collectionPath = "C:\Workspace\ConcurrencyChallenge\e2e\postman.json"

# Record the start time
$startTime = Get-Date

# Start the Newman instances in parallel
$jobs = @()
for ($i = 1; $i -le $concurrency; $i++) {
    $tempCollectionPath = "C:\Workspace\ConcurrencyChallenge\e2e\temp-collection-$i.json"
    # Copy the collection to avoid file conflicts
    Copy-Item -Path $collectionPath -Destination $tempCollectionPath

    $jobs += Start-Job -ScriptBlock {
        param($instanceNumber, $iterationsPerInstance, $tempCollectionPath)
        
        function Run-NewmanInstance {
            param (
                [int]$InstanceNumber,
                [string]$TempCollectionPath
            )
            Write-Host "Starting Newman Instance $InstanceNumber with $iterationsPerInstance iterations..."
            newman run $TempCollectionPath --iteration-count $iterationsPerInstance
            Write-Host "Newman Instance $InstanceNumber completed."
        }

        Run-NewmanInstance -InstanceNumber $instanceNumber -TempCollectionPath $tempCollectionPath
    } -ArgumentList $i, $iterationsPerInstance, $tempCollectionPath
}

# Wait for all the jobs to complete
Write-Host "Waiting for all Newman instances to complete..."
$jobs | ForEach-Object { Receive-Job -Job $_ -Wait }

# Record the end time
$endTime = Get-Date

# Calculate the duration
$duration = $endTime - $startTime
Write-Host "All concurrent Newman runs are done in $($duration.TotalSeconds) seconds."

# Cleanup: Remove temporary collections
Remove-Item ".\temp-collection-*.json" -Force

Write-Host "All concurrent Newman runs are done, and temporary files have been cleaned up."
