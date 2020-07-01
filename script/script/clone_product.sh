#!/bin/bash
#脚本负责拷贝bird/product/中的alps或idh目录
echo "================= clone_product.sh begin ==================="

OUTPUT_ROOT_DIR=.
#获取bird文件夹所在的相对路径
BIRD_DIR=${0%/bird/*}/bird
PRODUCTS_DIR=${BIRD_DIR}/product

if [ -d ${OUTPUT_ROOT_DIR}/vendor/mediatek ]; then
  MAIN_DIR_NAME="alps"
else
  MAIN_DIR_NAME="idh"
fi

if [ -d ${PRODUCTS_DIR}/common/${MAIN_DIR_NAME} ]; then
  echo "copy ${PRODUCTS_DIR}/common/${MAIN_DIR_NAME}"
  cp -a -f -T ${PRODUCTS_DIR}/common/${MAIN_DIR_NAME} ${OUTPUT_ROOT_DIR}
fi
if [ ${bird_product_folder} != "common" ]; then
  if [ -d ${PRODUCTS_DIR}/${bird_product_folder}/${MAIN_DIR_NAME} ]; then
    echo "copy ${PRODUCTS_DIR}/${bird_product_folder}/${MAIN_DIR_NAME}"
    cp -a -f -T ${PRODUCTS_DIR}/${bird_product_folder}/${MAIN_DIR_NAME} ${OUTPUT_ROOT_DIR}
  fi
fi

echo "=================  clone_product.sh end  ==================="
