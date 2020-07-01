#!/bin/bash
target_project=$1
drive_only=$2

OUTPUT_ROOT_DIR=.

if [ -d ${OUTPUT_ROOT_DIR}/vendor/mediatek ]; then
  mediatek=yes
else
  mediatek=no
fi
#获取bird文件夹所在的相对路径
BIRD_DIR=${0%/bird/*}/bird
#脚本文件夹所在的相对路径
SCRIPT_DIR=${BIRD_DIR}/script

# 项目mk
if [ $mediatek == "yes" ]; then
	if [ $bird_source_project == "k39tv1_bsp" ] || [ $bird_source_project == "k39tv1_64_bsp" ]; then
	    echo  "$bird_source_project "
  		cp ${BIRD_DIR}/mk/$target_project.mk ${OUTPUT_ROOT_DIR}/device/mediateksample/${bird_source_project}/ProjectConfig.mk
  	else	#bd_k61v1_64_bsp
  		cp ${BIRD_DIR}/mk/$target_project.mk ${OUTPUT_ROOT_DIR}/device/bird/${bird_source_project}/ProjectConfig.mk
  	fi		
else
  cp ${BIRD_DIR}/mk/${target_project}.mk ${OUTPUT_ROOT_DIR}/device/sprd/${bird_sprd}/${bird_project}/ProjectConfig.mk
fi

#GSM
if [ ${BUILD_GMS} ] && [ ${BUILD_GMS} == "yes" ]; then
  bash ${SCRIPT_DIR}/script/clone_gms.sh
fi

if [ $drive_only ] && [ ${drive_only} == "yes" ]; then
  echo "Build drive_only project!"
  # drive only只拷贝驱动文件
  # 驱动 注意：驱动拷贝脚本尚未统一到git中，目录不同
  bash ${SCRIPT_DIR}/clone_drv.sh
else
  # MMI
  bash ${SCRIPT_DIR}/script/clone_mmi.sh
  # 驱动 注意：驱动拷贝脚本尚未统一到git中，目录不同
  bash ${SCRIPT_DIR}/clone_drv.sh
  # 项目文件夹中的alps或idh目录
  bash ${SCRIPT_DIR}/script/clone_product.sh
fi


#仅USER版本执行否则无法刷机
bash ${SCRIPT_DIR}/script/bird_config_modify.sh
