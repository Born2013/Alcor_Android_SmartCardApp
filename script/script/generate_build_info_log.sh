#!/bin/bash
#此脚本负责生成编译相关的基本信息
lunch=$1

SAVE_PATH=${BIRD_BUILD_ROOT_DIR}/bird_build_info.log

echo "当前git节点(*开头的是当前分支): " | tee $SAVE_PATH
cd ${BIRD_DIR_PATH}
git branch -v | tee -a $SAVE_PATH
cd -
echo "" | tee -a $SAVE_PATH

echo "bird_target_project: $BIRD_BUILD_PROJECT_MK_NAME" | tee -a $SAVE_PATH
echo "mode: $BIRD_BUILD_MODE" | tee -a $SAVE_PATH
echo "lunch: $lunch" | tee -a $SAVE_PATH
echo "CUSTOM_HAL_MAIN_IMGSENSOR: $CUSTOM_HAL_MAIN_IMGSENSOR" | tee -a $SAVE_PATH
echo "CUSTOM_HAL_SUB_IMGSENSOR: $CUSTOM_HAL_SUB_IMGSENSOR" | tee -a $SAVE_PATH
