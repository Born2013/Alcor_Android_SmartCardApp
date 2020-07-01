#!/bin/bash
#lunch配置参数
lunch_name=$1
#指令
action=$2
#路径 mm等命令时用到
path=$3

echo "lunch: $lunch_name"
echo "action: $action"
echo "path: $path"

# 第一步 加载Android自带编译环境
source ./mbldenv.sh
source ./build/envsetup.sh
echo "PATH=${PATH}";
echo "ANDROID_JAVA_HOME=${ANDROID_JAVA_HOME}";
echo "JAVA_HOME=${JAVA_HOME}";

# 第二步 lunch
if [ -n "${lunch_name}" ] ;then
  lunch ${lunch_name}
  if [ $? != "0" ]; then
    echo -e "\033[31m lunch error! \033[0m"
    exit 1;
  fi
else
  echo -e "\033[31m lunch error:lunch arguments can not be null \033[0m"
  exit 1;
fi

# 第三步 按具体action执行
if [ $action == "new" ]; then
  make clean
  make -j8 2>&1 | tee build.log
elif [ $action == "r" ] || [ $action == "R" ]; then
  make -j8 2>&1 | tee build.log
elif [ $action == "snod" ]; then
  make -j8 snod 2>&1 | tee build.log
elif [ $action == "update-api" ]; then
  make -j8 update-api 2>&1 | tee build.log
elif [ $action == "lk" ]; then
  make -j8 bootloader 2>&1 | tee build.log
elif [ $action == "bootimage" ]; then
  make -j8 bootimage 2>&1 | tee build.log
elif [ $action == "recoveryimage" ]; then
  make -j8 recoveryimage 2>&1 | tee build.log
elif [ $action == "systemimage" ]; then
  make -j8 systemimage 2>&1 | tee build.log
elif [ $action == "userdataimage" ]; then
  make -j8 userdataimage 2>&1 | tee build.log
elif [ $action == "cacheimage" ]; then
  make -j8 cacheimage 2>&1 | tee build.log
elif [ $action == "clean" ]; then
  make clean
elif [ $action == "clobber" ]; then
  make clobber
elif [ $action == "clean-lk" ]; then
  make clean-lk
elif [ $action == "clean-kernel" ]; then
  make clean-kernel
elif [ $action == "mm" ]; then
  if [ -n "$path" ] ;then
    mmm $path 2>&1 | tee mm_build.log
  else
    echo -e "\033[31m 请指定模块路径 \033[0m"
    exit 1;
  fi
elif [ $action == "mma" ]; then
  if [ -n "$path" ] ;then
    mmma $path 2>&1 | tee mma_build.log
  else
    echo -e "\033[31m 请指定模块路径 \033[0m"
    exit 1;
  fi
elif [ $action == "ota" ]; then
  make clean
  make -j8 2>&1 | tee build.log
  if [ ${PIPESTATUS[0]} == "0" ]; then
    make otapackage 2>&1 | tee build_ota.log
  fi
else
  echo -e "\033[31m Unknown command \033[0m"
  exit 1;
fi

#通过PIPESTATUS来判断编译是否通过了
if [ ${PIPESTATUS[0]} == "0" ]; then
  exit 0;
else
  exit 1;
fi
