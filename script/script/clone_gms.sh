#!/bin/bash
echo "================= clone_gms.sh begin ==================="

if [ ! -n "${BIRD_GMS_VERSION}" ] ;then
  echo -e "\033[31m BIRD_GMS_VERSION is not defined \033[0m"
  exit 1;
fi

OUTPUT_ROOT_DIR=.
#获取bird文件夹所在的相对路径
BIRD_DIR=${0%/bird/*}/bird
PRODUCTS_DIR=${BIRD_DIR}/product
GSM_LN_DIR=${OUTPUT_ROOT_DIR}/../gms
GSM_SRC_DIR=${GSM_LN_DIR}/${BIRD_GMS_VERSION}
GSM_DEST_DIR=${OUTPUT_ROOT_DIR}/vendor/google

if [ ! -d "${GSM_LN_DIR}" ]; then
  if [ -n "${GMS_DIR}" ] ;then
    echo "GMS_DIR=${GMS_DIR}"
    ln -r -s -T "${GMS_DIR}" "${GSM_LN_DIR}"
  else
    mkdir "${GSM_LN_DIR}"
  fi
fi

if [ -d ${GSM_SRC_DIR} ]; then
  cd ${GSM_SRC_DIR}
  git reset --hard
  git pull origin master
  RESULTS=${PIPESTATUS[0]}
  cd -
else
  git clone git@192.168.50.55:../../var/GMS/${BIRD_GMS_VERSION} ${GSM_SRC_DIR}
  RESULTS=${PIPESTATUS[0]}
fi
if [ $RESULTS != "0" ]; then
  echo -e "\033[31m clone_gms.sh error \033[0m"
  exit 1;
fi

if [ -d ${GSM_DEST_DIR} ]; then
  rm -rf ${GSM_DEST_DIR}
fi
cp -a -f -T ${GSM_SRC_DIR}/google ${GSM_DEST_DIR}

echo "=================  clone_gms.sh end  ==================="
