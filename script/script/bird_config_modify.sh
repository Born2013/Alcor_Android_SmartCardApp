#仅USER版本执行否则无法刷机
echo "if build type is ud,Modify the following to facilitate software adb remount access!"
sed -i "s/ATTR_SUSBDL_ONLY_ENABLE_ON_SCHIP/ATTR_SUSBDL_DISABLE/g" ../alps/vendor/mediatek/proprietary/bootable/bootloader/preloader/custom/bd_k61v1_64_bsp/bd_k61v1_64_bsp.mk
sed -i "s/ATTR_SBOOT_ENABLE/ATTR_SBOOT_DISABLE/g" ../alps/vendor/mediatek/proprietary/bootable/bootloader/preloader/custom/bd_k61v1_64_bsp/bd_k61v1_64_bsp.mk
sed -i "s/ATTR_SUSBDL_ONLY_ENABLE_ON_SCHIP/ATTR_SUSBDL_DISABLE/g" ../alps/vendor/mediatek/proprietary/bootable/bootloader/preloader/custom/bd_k62v1_64_bsp/bd_k62v1_64_bsp.mk
sed -i "s/ATTR_SBOOT_ENABLE/ATTR_SBOOT_DISABLE/g" ../alps/vendor/mediatek/proprietary/bootable/bootloader/preloader/custom/bd_k62v1_64_bsp/bd_k62v1_64_bsp.mk
#仅USER版本执行否则无法刷机
