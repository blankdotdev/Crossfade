#!/bin/bash
# Strip signingConfigs block
sed -i '/signingConfigs {/,/    }/d' app/build.gradle.kts

# Strip the usage of signingConfigs in buildTypes
sed -i '/val releaseSigning = signingConfigs/d' app/build.gradle.kts
sed -i '/if (releaseSigning.storeFile != null) {/,/            }/d' app/build.gradle.kts

echo "Gradle file patched successfully."
