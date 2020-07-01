#!/bin/bash
echo "================= copy_and_pac.sh begin ==================="

COPY_DEST_DIR=${BIRD_DIR_PATH}/../$BIRD_BUILD_PROJECT_MK_NAME

if [ -d ${BIRD_BUILD_ROOT_DIR}/vendor/mediatek ]; then
  mediatek="yes"
else
  mediatek="no"
fi

#保留上一次的
if [ -d ${COPY_DEST_DIR}_old ]; then
  rm -rf ${COPY_DEST_DIR}_old
fi
if [ -d ${COPY_DEST_DIR} ]; then
  mv ${COPY_DEST_DIR} ${COPY_DEST_DIR}_old
fi

mkdir -p $COPY_DEST_DIR

echo "mediatek=$mediatek"
#MTK平台
if [ $mediatek == "yes" ]; then
  #modem
  mkdir -p ${COPY_DEST_DIR}/database
  cp ${BIRD_BUILD_ROOT_DIR}/out/target/product/*/obj/CGEN/APDB_* ${COPY_DEST_DIR}/database
  cp ${BIRD_BUILD_ROOT_DIR}/out/target/product/*/system/etc/mddb/MDDB* ${COPY_DEST_DIR}/database
  #Flash下载需要的文件
  cp ${BIRD_BUILD_ROOT_DIR}/out/target/product/*/*.img ${COPY_DEST_DIR}
  cp ${BIRD_BUILD_ROOT_DIR}/out/target/product/*/*.bin ${COPY_DEST_DIR}
  cp ${BIRD_BUILD_ROOT_DIR}/out/target/product/*/*Android_scatter.txt ${COPY_DEST_DIR}
  #OTA包
  if [ -e ${BIRD_BUILD_ROOT_DIR}/out/target/product/*/*-ota*.zip ]; then
    mkdir -p ${COPY_DEST_DIR}/ota
    cp ${BIRD_BUILD_ROOT_DIR}/out/target/product/*/*-ota*.zip ${COPY_DEST_DIR}/ota
  fi
  if [ -e ${BIRD_BUILD_ROOT_DIR}/out/target/product/*/target_files-package.zip ]; then
    #广升的OTA包
    mkdir -p ${COPY_DEST_DIR}/ota
    cp ${BIRD_BUILD_ROOT_DIR}/out/target/product/*/target_files-package.zip ${COPY_DEST_DIR}/ota
    cp ${BIRD_BUILD_ROOT_DIR}/out/target/product/*/obj/PACKAGING/target_files_intermediates/*-target_files*.zip ${COPY_DEST_DIR}/ota
  fi
  #项目mk
  mkdir -p ${COPY_DEST_DIR}/mk
  cp ${BIRD_DIR_PATH}/mk/${BIRD_BUILD_PROJECT_MK_NAME}.* ${COPY_DEST_DIR}/mk
  #bird_build_info.log
  cp ${BIRD_BUILD_ROOT_DIR}/bird_build_info.log ${COPY_DEST_DIR}/mk
fi

echo "================== copy_and_pac.sh end ===================="
