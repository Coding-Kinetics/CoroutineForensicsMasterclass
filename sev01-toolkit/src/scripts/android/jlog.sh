# ==============================================================================
# Copyright (c) 2026 Coding Kinetics LLC. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# ==============================================================================

jlog.sh

# ==============================================================================
# WHAT IS THIS FOR?
# ==============================================================================
# - Creates text logs of your Android device at runtime to make it easy to provide
# and share logs with others. Sample use:
#
# > ./jlog.sh ${name_of_script}
# > hit CTRL + C once complete and then retrieve the file


#!/bin/bash -e
# TODO replace with your name
adb='/Users/${INSERT_USERNAME}/Library/Android/sdk/platform-tools/adb'

openLog() {
        echo "opening $1"
        open $1
}

createDir() {
        if [ -d "$1" ]; then
                echo "$1 folder exists"
        else
                echo "$1 folder does not exist"
                mkdir $1
        fi
}

LOG_ROOT=~/dev/logs/
LOG_NAME="$1.log"
DUMP_LOG=false

if [ "$1" == "-d" ]; then
        echo "dump log"
        LOG_NAME="$2_dump.log"
        DUMP_LOG=true
fi

DIR="$LOG_ROOT`date +"%Y/%b/%d"`"
echo "TEST DATE: $DIR"
mkdir -p $DIR

LOG_FILE="$DIR/$LOG_NAME"
echo "filename: $LOG_NAME"
echo "$LOG_FILE"


# opens the log in the default app when execution is terminated
trap 'openLog $LOG_FILE' exit

if [ "$DUMP_LOG" = true ]; then
        echo "dumping"
        $adb logcat -d > $LOG_FILE
else
        echo "logging"
        $adb logcat -c
        $adb logcat -P ""
        $adb logcat > $LOG_FILE
fi