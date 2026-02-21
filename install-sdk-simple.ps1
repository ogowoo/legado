
$sdk = "$env:LOCALAPPDATA\Android\Sdk"
New-Item -ItemType Directory -Force -Path $sdk | Out-Null


$url = "https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip"
$zip = "$env:TEMP\cmdline-tools.zip"
# Invoke-WebRequest -Uri $url -OutFile $zip
# Expand-Archive -Path $zip -DestinationPath $env:TEMP -Force


New-Item -ItemType Directory -Force -Path "$sdk\cmdline-tools" | Out-Null
Move-Item -Path "$env:TEMP\cmdline-tools" -Destination "$sdk\cmdline-tools\latest" -Force


[Environment]::SetEnvironmentVariable("ANDROID_HOME", $sdk, "User")
$env:ANDROID_HOME = $sdk


& "$sdk\cmdline-tools\latest\bin\sdkmanager.bat" "platforms;android-36" "platform-tools" "build-tools;36.0.0"


"sdk.dir=$sdk" | Set-Content local.properties
Write-Host "done, please run: .\gradlew.bat assembleDebug"