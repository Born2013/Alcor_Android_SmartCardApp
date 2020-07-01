#!/bin/bash
echo "================= clone_mmi.sh begin ==================="

OUTPUT_ROOT_DIR=.
#获取bird文件夹所在的相对路径
BIRD_DIR=${0%/bird/*}/bird
PRODUCTS_DIR=${BIRD_DIR}/product
MMI_DIR=${BIRD_DIR}/mmi

if [ -d ${OUTPUT_ROOT_DIR}/vendor/mediatek ]; then
  mediatek=yes
  MAIN_DIR_NAME="alps"
else
  mediatek=no
  MAIN_DIR_NAME="idh"
fi

#判断bird目录是在alps或idh里面还是外面。两种情况存在拷贝上的差异
if [ $BIRD_DIR == "./bird" ]; then
  BIRD_IS_INSIDE="yes"
else
  BIRD_IS_INSIDE="no"
fi

echo BIRD_IS_INSIDE=$BIRD_IS_INSIDE

if [ -n "${bird_product_folder}" ] ;then
  echo "clone_mmi.sh => current product is ${bird_product_folder}"
else
  bird_product_folder=common
  echo "clone_mmi.sh => current product is null, so to be ${bird_product_folder}"
fi
echo "clone_mmi.sh => current BOOT_LOGO is ${BOOT_LOGO}"

#1. 拷贝bird目录中的alps或idh
bird_temp_dir=${OUTPUT_ROOT_DIR}/../bird_temp
if [ -d ${bird_temp_dir} ]; then
  rm -rf ${bird_temp_dir}
fi
cp -a -f -T ${BIRD_DIR}/${MAIN_DIR_NAME} ${bird_temp_dir}
if [ -f ${bird_temp_dir}/Android.mk ]; then
  rm ${bird_temp_dir}/Android.mk
fi
cp -a -f -T ${bird_temp_dir} ${OUTPUT_ROOT_DIR}
rm -rf ${bird_temp_dir}

#2. copy wallpaper
#默认桌面壁纸
default_wallpaper_dest_dir=${OUTPUT_ROOT_DIR}/frameworks/base/core/res/res/drawable-nodpi
if [ -f ${PRODUCTS_DIR}/${bird_product_folder}/BirdWallPaper/default/${BOOT_LOGO}/default_wallpaper.* ]; then
  default_wallpaper_src=${PRODUCTS_DIR}/${bird_product_folder}/BirdWallPaper/default/${BOOT_LOGO}/default_wallpaper.*
elif [ -f ${PRODUCTS_DIR}/common/BirdWallPaper/default/${BOOT_LOGO}/default_wallpaper.* ]; then
  default_wallpaper_src=${PRODUCTS_DIR}/common/BirdWallPaper/default/${BOOT_LOGO}/default_wallpaper.*
fi
if [ -n "${default_wallpaper_src}" ] ;then
  if [ -d "${OUTPUT_ROOT_DIR}/device/sprd" ] ;then
    find ${OUTPUT_ROOT_DIR}/device/sprd -name "default_wallpaper.*" -exec rm -f {} \;
  fi
  if [ -d "${OUTPUT_ROOT_DIR}/device/mediatek" ] ;then
    find ${OUTPUT_ROOT_DIR}/device/mediatek -name "default_wallpaper.*" -exec rm -f {} \;
  fi
  find ${OUTPUT_ROOT_DIR}/frameworks/base/core/res/res -name "default_wallpaper.*" -exec rm -f {} \;
  cp -a -f ${default_wallpaper_src} ${default_wallpaper_dest_dir}
fi
#默认锁屏壁纸
if [ $mediatek == "yes" ]; then
  default_lockscreen_wallpaper_dest_parent_dir=${OUTPUT_ROOT_DIR}/framework-bird/res/res
else
  default_lockscreen_wallpaper_dest_parent_dir=${OUTPUT_ROOT_DIR}/frameworks/base/core/res/res
fi
default_lockscreen_wallpaper_dest_dir=$default_lockscreen_wallpaper_dest_parent_dir/drawable-nodpi
if [ -f ${PRODUCTS_DIR}/${bird_product_folder}/BirdWallPaper/default/${BOOT_LOGO}/*default_lockscreen.* ]; then
  default_lockscreen_wallpaper_src=${PRODUCTS_DIR}/${bird_product_folder}/BirdWallPaper/default/${BOOT_LOGO}/*default_lockscreen.*
elif [ -f ${PRODUCTS_DIR}/common/BirdWallPaper/default/${BOOT_LOGO}/*default_lockscreen.* ]; then
  default_lockscreen_wallpaper_src=${PRODUCTS_DIR}/common/BirdWallPaper/default/${BOOT_LOGO}/*default_lockscreen.*
fi
if [ -n "${default_lockscreen_wallpaper_src}" ] ;then
  find $default_lockscreen_wallpaper_dest_parent_dir -name "default_lockscreen.*" -exec rm -f {} \;
  find $default_lockscreen_wallpaper_dest_parent_dir -name "zzzzz_default_lockscreen.*" -exec rm -f {} \;
  mkdir -p ${default_lockscreen_wallpaper_dest_dir}
  cp -a -f ${default_lockscreen_wallpaper_src} ${default_lockscreen_wallpaper_dest_dir}
fi
#预置壁纸
#wallpapers.xml
wallpapers_xml_dest=${OUTPUT_ROOT_DIR}/packages/apps/WallpaperPicker/res/values-nodpi
mtk_wallpapers_xml_dest=${OUTPUT_ROOT_DIR}/vendor/mediatek/proprietary/packages/apps/WallpaperPicker/res/values-nodpi
bird_wallpapers_xml_dest=${MMI_DIR}/bird_app/BirdWallpaperPicker/res/values-nodpi
if [ -f ${PRODUCTS_DIR}/${bird_product_folder}/BirdWallPaper/preload/values/wallpapers.xml ]; then
  wallpapers_xml_src=${PRODUCTS_DIR}/${bird_product_folder}/BirdWallPaper/preload/values/wallpapers.xml
elif [ -f ${PRODUCTS_DIR}/common/BirdWallPaper/preload/values/wallpapers.xml ]; then
  wallpapers_xml_src=${PRODUCTS_DIR}/common/BirdWallPaper/preload/values/wallpapers.xml
fi
if [ -n "${wallpapers_xml_src}" ] ;then
  cp -a -f ${wallpapers_xml_src} ${wallpapers_xml_dest}
  if [ -d ${mtk_wallpapers_xml_dest} ]; then
    cp -a -f ${wallpapers_xml_src} ${mtk_wallpapers_xml_dest}
  fi
  if [ -d ${bird_wallpapers_xml_dest} ]; then
    cp -a -f ${wallpapers_xml_src} ${bird_wallpapers_xml_dest}
  fi
fi
#图片
android_wallpapers_res_dir=${OUTPUT_ROOT_DIR}/packages/apps/WallpaperPicker/res
mtk_wallpapers_res_dir=${OUTPUT_ROOT_DIR}/vendor/mediatek/proprietary/packages/apps/WallpaperPicker/res
bird_wallpapers_res_dir=${MMI_DIR}/bird_app/BirdWallpaperPicker/res
if [ -d ${PRODUCTS_DIR}/${bird_product_folder}/BirdWallPaper/preload/${BOOT_LOGO} ]; then
  wallpapers_drawable_src_dir=${PRODUCTS_DIR}/${bird_product_folder}/BirdWallPaper/preload/${BOOT_LOGO}
elif [ -d ${PRODUCTS_DIR}/common/BirdWallPaper/preload/${BOOT_LOGO} ]; then
  wallpapers_drawable_src_dir=${PRODUCTS_DIR}/common/BirdWallPaper/preload/${BOOT_LOGO}
fi
if [ -n "${wallpapers_drawable_src_dir}" ] ;then
  if [ -d ${android_wallpapers_res_dir} ]; then
    cp -a -f -T ${wallpapers_drawable_src_dir} ${android_wallpapers_res_dir}/drawable-nodpi
  fi
  if [ -d ${mtk_wallpapers_res_dir} ]; then
    cp -a -f -T ${wallpapers_drawable_src_dir} ${mtk_wallpapers_res_dir}/drawable-nodpi
  fi
  if [ -d ${bird_wallpapers_res_dir} ]; then
    cp -a -f -T ${wallpapers_drawable_src_dir} ${bird_wallpapers_res_dir}/drawable-nodpi
  fi
fi


#3. copy LOGO
# uboot logo
if [ -f ${PRODUCTS_DIR}/${bird_product_folder}/boot_logo/${BOOT_LOGO}/uboot_logo.bmp ]; then
  uboot_logo_src=${PRODUCTS_DIR}/${bird_product_folder}/boot_logo/${BOOT_LOGO}/uboot_logo.bmp
elif [ -f ${PRODUCTS_DIR}/common/boot_logo/${BOOT_LOGO}/uboot_logo.bmp ]; then
  uboot_logo_src=${PRODUCTS_DIR}/common/boot_logo/${BOOT_LOGO}/uboot_logo.bmp
fi
if [ $mediatek == "yes" ]; then
  uboot_logo_dest=${OUTPUT_ROOT_DIR}/vendor/mediatek/proprietary/bootable/bootloader/lk/dev/logo/${BOOT_LOGO}/${BOOT_LOGO}_uboot.bmp
else
  mkdir -p ${OUTPUT_ROOT_DIR}/device/sprd/${bird_sprd}/${bird_project}/modem_bins
  uboot_logo_dest=${OUTPUT_ROOT_DIR}/device/sprd/${bird_sprd}/${bird_project}/modem_bins/logo1.bmp
fi
if [ -n "${uboot_logo_src}" ] ;then
  cp -rf ${uboot_logo_src} ${uboot_logo_dest}
fi
# kernel logo
if [ -f ${PRODUCTS_DIR}/${bird_product_folder}/boot_logo/${BOOT_LOGO}/kernel_logo.bmp ]; then
  kernel_logo_src=${PRODUCTS_DIR}/${bird_product_folder}/boot_logo/${BOOT_LOGO}/kernel_logo.bmp
elif [ -f ${PRODUCTS_DIR}/common/boot_logo/${BOOT_LOGO}/kernel_logo.bmp ]; then
  kernel_logo_src=${PRODUCTS_DIR}/common/boot_logo/${BOOT_LOGO}/kernel_logo.bmp
fi
if [ $mediatek == "yes" ]; then
  kernel_logo_dest=${OUTPUT_ROOT_DIR}/vendor/mediatek/proprietary/bootable/bootloader/lk/dev/logo/${BOOT_LOGO}/${BOOT_LOGO}_kernel.bmp
else
  mkdir -p ${OUTPUT_ROOT_DIR}/device/sprd/${bird_sprd}/${bird_project}/modem_bins
  kernel_logo_dest=${OUTPUT_ROOT_DIR}/device/sprd/${bird_sprd}/${bird_project}/modem_bins/logo2.bmp
fi
if [ -n "${kernel_logo_src}" ] ;then
  cp -rf ${kernel_logo_src} ${kernel_logo_dest}
fi


#4. copy boot_animation
bootanimation_dest=${OUTPUT_ROOT_DIR}/frameworks/base/data/sounds/bootanimation.zip
if [ -f ${PRODUCTS_DIR}/${bird_product_folder}/boot_animation/${BOOT_LOGO}/bootanimation.zip ]; then
  bootanimation_src=${PRODUCTS_DIR}/${bird_product_folder}/boot_animation/${BOOT_LOGO}/bootanimation.zip
elif [ -f ${PRODUCTS_DIR}/common/boot_animation/${BOOT_LOGO}/bootanimation.zip ]; then
  bootanimation_src=${PRODUCTS_DIR}/common/boot_animation/${BOOT_LOGO}/bootanimation.zip
fi
if [ -n "${bootanimation_src}" ] ;then
  cp -rf ${bootanimation_src} ${bootanimation_dest}
fi


#5. copy shut_animation
if [ $mediatek == "yes" ]; then
  shutdownanimation_dest=${OUTPUT_ROOT_DIR}/frameworks/base/data/sounds/shutanimation.zip
else
  shutdownanimation_dest=${OUTPUT_ROOT_DIR}/frameworks/base/data/sounds/shutdownanimation.zip
fi
if [ -f ${PRODUCTS_DIR}/${bird_product_folder}/shut_animation/${BOOT_LOGO}/shutdownanimation.zip ]; then
  shutdownanimation_src=${PRODUCTS_DIR}/${bird_product_folder}/shut_animation/${BOOT_LOGO}/shutdownanimation.zip
elif [ -f ${PRODUCTS_DIR}/common/shut_animation/${BOOT_LOGO}/shutdownanimation.zip ]; then
  shutdownanimation_src=${PRODUCTS_DIR}/common/shut_animation/${BOOT_LOGO}/shutdownanimation.zip
fi
if [ -n "${shutdownanimation_src}" ] ;then
  cp -rf ${shutdownanimation_src} ${shutdownanimation_dest}
fi


#6. copy sounds
sounds_dest_dir=${OUTPUT_ROOT_DIR}/frameworks/base/data/sounds
cp -a -f -T ${PRODUCTS_DIR}/common/sounds ${sounds_dest_dir}
if [ ${bird_product_folder} != "common" ]; then
  if [ -d ${PRODUCTS_DIR}/${bird_product_folder}/sounds ]; then
    cp -a -f -T ${PRODUCTS_DIR}/${bird_product_folder}/sounds ${sounds_dest_dir}
  fi
fi
#boot_shut_audios
cp -a -f -T ${PRODUCTS_DIR}/common/boot_shut_audios ${sounds_dest_dir}
if [ ${bird_product_folder} != "common" ]; then
	if [ -d ${PRODUCTS_DIR}/${bird_product_folder}/boot_shut_audios ]; then
		cp -a -f -T ${PRODUCTS_DIR}/${bird_product_folder}/boot_shut_audios ${sounds_dest_dir}
	fi
fi


#7. 拷贝3rd_party app
if [ $BIRD_IS_INSIDE == "yes" ]; then
  third_apps_dest_dir=${MMI_DIR}/3rd_party
else
  third_apps_dest_dir=${OUTPUT_ROOT_DIR}/packages/3rd_party
  if [ -d ${third_apps_dest_dir} ]; then
    rm -rf ${third_apps_dest_dir}
  fi
  if [ -d ${MMI_DIR}/3rd_party ]; then
    cp -a -f -T ${MMI_DIR}/3rd_party ${third_apps_dest_dir}
  fi
fi
mkdir -p ${third_apps_dest_dir}/apk_pool
if [ -d ${PRODUCTS_DIR}/${bird_product_folder}/apk_pool ]; then
  cp -a -f -T ${PRODUCTS_DIR}/${bird_product_folder}/apk_pool/ ${third_apps_dest_dir}/apk_pool
else
  cp -a -f -T ${PRODUCTS_DIR}/common/apk_pool/ ${third_apps_dest_dir}/apk_pool
fi


#8. 拷贝bird app
if [ $BIRD_IS_INSIDE == "no" ]; then
  bird_app_dest_dir=${OUTPUT_ROOT_DIR}/packages/bird_app
  if [ -d ${bird_app_dest_dir} ]; then
    rm -rf ${bird_app_dest_dir}
  fi
  if [ -d ${MMI_DIR}/bird_app ]; then
    cp -a -f -T ${MMI_DIR}/bird_app ${bird_app_dest_dir}
  fi
else
  bird_app_dest_dir=${MMI_DIR}/bird_app
fi


#9. 拷贝独立的app
#~ BIRD_GIT_APPS="MMITest"
#~ for app in $BIRD_GIT_APPS;
#~ do
  #~ if [ -d ${bird_app_dest_dir}/${app} ]; then
    #~ rm -rf ${bird_app_dest_dir}/${app}
  #~ fi
  #~ mkdir -p ${bird_app_dest_dir}/${app}
  #~ git clone git@192.168.50.55:../../var/BIRD_APP/${app} ${bird_app_dest_dir}/${app};
#~ done

# 预装应用 begin
if [ $BIRD_IS_INSIDE == "no" ]; then
  bird_app_dest_dir=${OUTPUT_ROOT_DIR}/packages/prebuild_app
  if [ -d ${bird_app_dest_dir} ]; then
    rm -rf ${bird_app_dest_dir}
  fi
  if [ -d ${MMI_DIR}/prebuild_app ]; then
    cp -a -f -T ${MMI_DIR}/prebuild_app ${bird_app_dest_dir}
  fi
  
  if [ ${bird_product_folder} != "common" ]; then
    if [ -d ${PRODUCTS_DIR}/${bird_product_folder}/prebuild_app ]; then
      cp -a -f -T ${PRODUCTS_DIR}/${bird_product_folder}/prebuild_app ${bird_app_dest_dir}
    fi
  fi

  bash ${bird_app_dest_dir}/prebuilt_mk.sh
fi

echo "=================  clone_mmi.sh end  ==================="
