#!/bin/bash

echo "clone DRV begin"

cd ${BIRD_DIR_PATH}

#choose source_project ... 
if [ -z ${bird_source_project} ]; then
  echo  "please choose one source project---forbid NULL !!!"
else
  echo  "you choose source project = ${bird_source_project}"
fi

echo "copy alps for driver..begin"
cp -a -f -T alps/device					     	  ../alps/device
cp -a -f -T alps/system					     	  ../alps/system
cp -a -f -T alps/vendor/mediatek/proprietary/bootable             ../alps/vendor/mediatek/proprietary/bootable 
cp -a -f -T alps/vendor/mediatek/proprietary/custom               ../alps/vendor/mediatek/proprietary/custom
cp -a -f -T alps/vendor/mediatek/proprietary/fingerprint	  ../alps/vendor/mediatek/proprietary/fingerprint
cp -a -f -T alps/vendor/mediatek/proprietary/modem		  ../alps/vendor/mediatek/proprietary/modem
#cp -a -f -T alps/vendor/mediatek/proprietary/frameworks		  ../alps/vendor/mediatek/proprietary/frameworks
cp -a -f -T alps/vendor/mediatek/proprietary/scripts		  ../alps/vendor/mediatek/proprietary/scripts
cp -a -f -T alps/vendor/mediatek/proprietary/msensor              ../alps/vendor/mediatek/proprietary/msensor 
if [ $bird_source_project == "k39tv1_bsp" ] || [ $bird_source_project == "k39tv1_64_bsp" ]; then
	echo  "you have choosen kernel-4.4 for $bird_source_project branch..."
	cp -a -f -T alps/kernel-4.4					  ../alps/kernel-4.4
else	
	echo  "you have choosen kernel-4.9 for $bird_source_project branch..."
	cp -a -f -T alps/kernel-4.9					  ../alps/kernel-4.9
fi	
echo "copy alps for driver..end"

#copy .mk and config
if [ -z ${bird_mk_config} ]; then
  echo  "bird_mk_config not config use default"
else
  if [ -d driver/mk_config/${bird_mk_config} ]; then
    cp -a -T  driver/mk_config/${bird_mk_config}/${bird_source_project}.mk  ../alps/vendor/mediatek/proprietary/bootable/bootloader/lk/project/${bird_source_project}.mk
    cp -a -T  driver/mk_config/${bird_mk_config}/${bird_source_project}_pl.mk  ../alps/vendor/mediatek/proprietary/bootable/bootloader/preloader/custom/${bird_source_project}/${bird_source_project}.mk

	if [ $bird_source_project == "k39tv1_bsp" ]; then
		cp -a -T  driver/mk_config/${bird_mk_config}/${bird_source_project}_debug_defconfig  ../alps/kernel-4.4/arch/arm/configs/${bird_source_project}_debug_defconfig
		cp -a -T  driver/mk_config/${bird_mk_config}/${bird_source_project}_defconfig  ../alps/kernel-4.4/arch/arm/configs/${bird_source_project}_defconfig
		echo  "copy mk and config H ${bird_mk_config} 32bit OK"
	elif [ $bird_source_project == "k39tv1_64_bsp" ]; then
		cp -a -T  driver/mk_config/${bird_mk_config}/${bird_source_project}_debug_defconfig  ../alps/kernel-4.4/arch/arm64/configs/${bird_source_project}_debug_defconfig
		cp -a -T  driver/mk_config/${bird_mk_config}/${bird_source_project}_defconfig  ../alps/kernel-4.4/arch/arm64/configs/${bird_source_project}_defconfig
		echo  "copy mk and config H ${bird_mk_config} 64bit OK"
	else
		cp -a -T  driver/mk_config/${bird_mk_config}/${bird_source_project}_debug_defconfig  ../alps/kernel-4.9/arch/arm64/configs/${bird_source_project}_debug_defconfig
		cp -a -T  driver/mk_config/${bird_mk_config}/${bird_source_project}_defconfig  ../alps/kernel-4.9/arch/arm64/configs/${bird_source_project}_defconfig
		echo  "copy mk and config H ${bird_mk_config} 64bit OK"
	fi	    
  fi
fi

#copy dws
if [ -z ${bird_dws} ]; then
  echo  "bird_dws not config use default"
else
  if [ -d driver/dws/${bird_dws} ]; then
    cp -a -T    driver/dws/${bird_dws}/codegen.dws ../alps/vendor/mediatek/proprietary/bootable/bootloader/lk/target/${bird_source_project}/dct/dct/codegen.dws
    cp -a -T    driver/dws/${bird_dws}/codegen.dws ../alps/vendor/mediatek/proprietary/bootable/bootloader/preloader/custom/${bird_source_project}/dct/dct/codegen.dws
    cp -a -T    driver/dws/${bird_dws}/codegen.dws ../alps/vendor/mediatek/proprietary/custom/${bird_source_project}/kernel/dct/dct/codegen.dws
    
	if [ $bird_source_project == "k39tv1_bsp" ] || [ $bird_source_project == "k39tv1_64_bsp" ]; then
		cp -a -T    driver/dws/${bird_dws}/codegen.dws ../alps/kernel-4.4/drivers/misc/mediatek/dws/mt6739/${bird_source_project}.dws
		echo  "copy dws H ${bird_dws} $bird_source_project OK"	
	elif [ $bird_source_project == "bd_k61v1_64_bsp" ]; then
			cp -a -T    driver/dws/${bird_dws}/codegen.dws ../alps/kernel-4.9/drivers/misc/mediatek/dws/mt6761/${bird_source_project}.dws
	else	#bd_k62v1_64_bsp
			cp -a -T    driver/dws/${bird_dws}/codegen.dws ../alps/kernel-4.9/drivers/misc/mediatek/dws/mt6765/${bird_source_project}.dws
	fi	        
  fi   
fi

#copy dts custom
if [ -z ${bird_dts} ]; then
  echo  "bird_dts not config use default"
else
  if [ -d driver/dts/${bird_dts} ]; then
 	 if [ -d driver/dts/${bird_dts} ]; then
		if [ $bird_source_project == "k39tv1_bsp" ]; then
			cp -a -T   driver/dts/${bird_dts}    ../alps/kernel-4.4/arch/arm/boot/dts
			echo  "copy dts  H ${bird_dts} 32bit OK"
		elif [ $bird_source_project == "k39tv1_64_bsp" ]; then
			cp -a -T   driver/dts/${bird_dts}    ../alps/kernel-4.4/arch/arm64/boot/dts/mediatek
			echo  "copy dts  H ${bird_dts} 64bit OK"
		else
			cp -a -T   driver/dts/${bird_dts}    ../alps/kernel-4.9/arch/arm64/boot/dts/mediatek
			echo  "copy dts  H ${bird_dts} 64bit OK"
		fi		
 	 fi 
 	   	 
    fi
fi

#copy memory
if [ -z ${bird_memory_flash} ]; then
  echo  "bird_memory size not config use default"
else
  if [ -d driver/memory/${bird_memory_flash} ]; then
    cp -a -T    driver/memory/${bird_memory_flash}/custom_MemoryDevice.h    ../alps/vendor/mediatek/proprietary/bootable/bootloader/preloader/custom/${bird_source_project}/inc/custom_MemoryDevice.h
    echo  "copy memory  H ${bird_memory_flash} OK"
  fi   
fi

#copy camera_hw
if [ -z ${bird_camera_hw} ]; then
  echo  "bird_camera_hw not config use default"
else
  if [ -d driver/camera_hw/${bird_camera_hw} ]; then

	if [ $bird_source_project == "k39tv1_bsp" ] || [ $bird_source_project == "k39tv1_64_bsp" ]; then
		cp -a -T    driver/camera_hw/${bird_camera_hw}    ../alps/kernel-4.4/drivers/misc/mediatek/imgsensor/src/mt6739/camera_hw
		echo  "copy camera_hw  H ${bird_camera_hw} OK"
	elif [ $bird_source_project == "bd_k61v1_64_bsp" ]; then
		cp -a -T    driver/camera_hw/${bird_camera_hw}    ../alps/kernel-4.9/drivers/misc/mediatek/imgsensor/src/mt6761/camera_hw
	else	#bd_k62v1_64_bsp
		cp -a -T    driver/camera_hw/${bird_camera_hw}    ../alps/kernel-4.9/drivers/misc/mediatek/imgsensor/src/mt6765/camera_hw
	fi	            
  fi   
fi

#copy custom main & sub camera
if [ -z ${bird_camera_effect_para} ]; then
  echo  "bird_camera_effect_para not config use default"
else
  if [ -d driver/camerapara/${bird_camera_effect_para} ]; then
	if [ $bird_source_project == "k39tv1_bsp" ] || [ $bird_source_project == "k39tv1_64_bsp" ]; then
		cp -a -T    driver/camerapara/${bird_camera_effect_para}/hal/		  ../alps/vendor/mediatek/proprietary/custom/mt6739/hal
		cp -a -T    driver/camerapara/${bird_camera_effect_para}/kernel/imgsensor/  ../alps/kernel-4.4/drivers/misc/mediatek/imgsensor/src/common/v1
		cp -a  -T   driver/camerapara/${bird_camera_effect_para}/kernel/lens        ../alps/kernel-4.4/drivers/misc/mediatek/lens/main/common
		echo  "copy bird_camera_effect_para H ${bird_camera_effect_para} OK"	
	elif [ $bird_source_project == "bd_k61v1_64_bsp" ]; then
		cp -a -T    driver/camerapara/${bird_camera_effect_para}/hal/		  ../alps/vendor/mediatek/proprietary/custom/mt6761/hal
		cp -a -T    driver/camerapara/${bird_camera_effect_para}/kernel/imgsensor/  ../alps/kernel-4.9/drivers/misc/mediatek/imgsensor/src/common/v1
		cp -a  -T   driver/camerapara/${bird_camera_effect_para}/kernel/lens        ../alps/kernel-4.9/drivers/misc/mediatek/lens/main/common
	else	#bd_k62v1_64_bsp
		cp -a -T    driver/camerapara/${bird_camera_effect_para}/hal/		  ../alps/vendor/mediatek/proprietary/custom/mt6765/hal
		cp -a -T    driver/camerapara/${bird_camera_effect_para}/kernel/imgsensor/  ../alps/kernel-4.9/drivers/misc/mediatek/imgsensor/src/common/v1
		cp -a  -T   driver/camerapara/${bird_camera_effect_para}/kernel/lens        ../alps/kernel-4.9/drivers/misc/mediatek/lens/main/common			
		echo  "copy bird_camera_effect_para H ${bird_camera_effect_para} OK"	
	fi
  fi
fi

#copy audiopara
if [ -z ${bird_audio_effect_para} ]; then
	echo  "bird_audio_effect_para not config use default"
else
	if [ -d driver/audiopara/${bird_audio_effect_para} ]; then
		cp -a -f -T driver/audiopara/${bird_audio_effect_para}  ../alps/device/mediatek/common/audio_param
	fi
    echo  "copy audio_effect_para  H ${bird_audio_effect_para} OK"
fi

#copy bird_gps_clock_type
if [ -z ${bird_wifi_band_type} ]; then
  echo  "bird_wifi_band_type not config use default"
else
  if [ -d driver/wifi_band_type/${bird_wifi_band_type} ]; then
    cp -a -T    driver/wifi_band_type/${bird_wifi_band_type}/CFG_WIFI_Default.h    ../alps/vendor/mediatek/proprietary/custom/${bird_source_project}/cgen/cfgdefault/CFG_WIFI_Default.h
  fi   
      echo  "copy bird_wifi_band_type  H ${bird_wifi_band_type} OK"
fi

#copy battery para
if [ -z ${bird_battery_para} ]; then
	echo  "bird_battery_para not config use default"
else
	if [ -d driver/battery/${bird_battery_para} ]; then
		if [ $bird_source_project == "k39tv1_bsp" ]; then
			cp -a -T    driver/battery/${bird_battery_para}/mt6739_battery_prop_ext.dtsi    ../alps/kernel-4.4/arch/arm/boot/dts/bat_setting/mt6739_battery_prop_ext.dtsi
			cp -a -T    driver/battery/${bird_battery_para}/mt6739_battery_table_ext.dtsi   ../alps/kernel-4.4/arch/arm/boot/dts/bat_setting/mt6739_battery_table_ext.dtsi
			cp -a -T    driver/battery/${bird_battery_para}/mtk_battery_table.h   ../alps/kernel-4.4/drivers/misc/mediatek/include/mt-plat/mt6739/include/mach/mtk_battery_table.h
			cp -a -T    driver/battery/${bird_battery_para}/mtk_charging.h   ../alps/kernel-4.4/drivers/misc/mediatek/include/mt-plat/mt6739/include/mach/mtk_charging.h
		elif [ $bird_source_project == "k39tv1_64_bsp" ]; then
			cp -a -T    driver/battery/${bird_battery_para}/mt6739_battery_prop_ext.dtsi    ../alps/kernel-4.4/arch/arm64/boot/dts/mediatek/bat_setting/mt6739_battery_prop_ext.dtsi
			cp -a -T    driver/battery/${bird_battery_para}/mt6739_battery_table_ext.dtsi   ../alps/kernel-4.4/arch/arm64/boot/dts/mediatek/bat_setting/mt6739_battery_table_ext.dtsi
			cp -a -T    driver/battery/${bird_battery_para}/mtk_battery_table.h   ../alps/kernel-4.4/drivers/misc/mediatek/include/mt-plat/mt6739/include/mach/mtk_battery_table.h
			cp -a -T    driver/battery/${bird_battery_para}/mtk_charging.h   ../alps/kernel-4.4/drivers/misc/mediatek/include/mt-plat/mt6739/include/mach/mtk_charging.h
		elif [ $bird_source_project == "bd_k61v1_64_bsp" ]; then
			cp -a -T    driver/battery/${bird_battery_para}/mt6761_battery_prop_ext.dtsi    ../alps/kernel-4.9/arch/arm64/boot/dts/mediatek/bat_setting/mt6761_battery_prop_ext.dtsi
			cp -a -T    driver/battery/${bird_battery_para}/mt6761_battery_table_ext.dtsi   ../alps/kernel-4.9/arch/arm64/boot/dts/mediatek/bat_setting/mt6761_battery_table_ext.dtsi
			cp -a -T    driver/battery/${bird_battery_para}/mtk_battery_table.h   ../alps/kernel-4.9/drivers/misc/mediatek/include/mt-plat/mt6761/include/mach/mtk_battery_table.h
			cp -a -T    driver/battery/${bird_battery_para}/mtk_charging.h   ../alps/kernel-4.9/drivers/misc/mediatek/include/mt-plat/mt6761/include/mach/mtk_charging.h
		else	#bd_k62v1_64_bsp
			cp -a -T    driver/battery/${bird_battery_para}/mt6765_battery_prop_ext.dtsi    ../alps/kernel-4.9/arch/arm64/boot/dts/mediatek/bat_setting/mt6765_battery_prop_ext.dtsi
			cp -a -T    driver/battery/${bird_battery_para}/mt6765_battery_table_ext.dtsi   ../alps/kernel-4.9/arch/arm64/boot/dts/mediatek/bat_setting/mt6765_battery_table_ext.dtsi
			cp -a -T    driver/battery/${bird_battery_para}/mtk_battery_table.h   ../alps/kernel-4.9/drivers/misc/mediatek/include/mt-plat/mt6765/include/mach/mtk_battery_table.h
			cp -a -T    driver/battery/${bird_battery_para}/mtk_charger_init.h   ../alps/kernel-4.9/drivers/misc/mediatek/include/mt-plat/mt6765/include/mach/mtk_charger_init.h			
		fi	
      echo  "copy battery capacity ${bird_battery_para} OK"	
	fi   
fi


	
if [ -z ${bird_touchscreen} ]; then
	echo  "bird_touchscreen not config use default"
else
   if [ $bird_source_project == "k39tv1_64_bsp" ]; then
		if [ -d driver/touchscreen/${bird_touchscreen} ]; then
			cp -a -T    driver/touchscreen/${bird_touchscreen}    ../alps/kernel-4.4/drivers/input/touchscreen/mediatek/${bird_touchscreen}
		fi   
		echo  "copy bird_touchscreen  H ${bird_touchscreen} OK" 
	fi
fi

#copy bird_thermal_type
if [ -z ${bird_thermal_type} ]; then
	echo  "bird_thermal_type not config use default = no limit"	
	if [ $bird_source_project == "k39tv1_bsp" ] || [ $bird_source_project == "k39tv1_64_bsp" ]; then
		cp -a -T    driver/thermal_type/mt6739/thermal_for_no_limit/thermal.conf    ../alps/device/mediatek/mt6739/thermal.conf
		cp -a -T    driver/thermal_type/mt6739/thermal_for_no_limit/thermal.eng.conf    ../alps/device/mediatek/mt6739/thermal.eng.conf
	elif [ $bird_source_project == "bd_k61v1_64_bsp" ]; then
		cp -a -T    driver/thermal_type/mt6761/thermal_for_no_limit/thermal.conf    ../alps/device/mediatek/mt6761/thermal.conf
		cp -a -T    driver/thermal_type/mt6761/thermal_for_no_limit/thermal.eng.conf    ../alps/device/mediatek/mt6761/thermal.eng.conf
	else	#bd_k62v1_64_bsp
		cp -a -T    driver/thermal_type/mt6765/thermal_for_no_limit/thermal.conf    ../alps/device/mediatek/mt6765/thermal.conf
		cp -a -T    driver/thermal_type/mt6765/thermal_for_no_limit/thermal.eng.conf    ../alps/device/mediatek/mt6765/thermal.eng.conf		
	fi	              
else
  if [ -d driver/thermal_type/${bird_thermal_type} ]; then
	if [ $bird_source_project == "k39tv1_bsp" ] || [ $bird_source_project == "k39tv1_64_bsp" ]; then
		cp -a -T    driver/thermal_type/mt6739/${bird_thermal_type}/thermal.conf    ../alps/device/mediatek/mt6739/thermal.conf
		cp -a -T    driver/thermal_type/mt6739/${bird_thermal_type}/thermal.eng.conf    ../alps/device/mediatek/mt6739/thermal.eng.conf
	elif [ $bird_source_project == "bd_k61v1_64_bsp" ]; then
		cp -a -T    driver/thermal_type/mt6761/${bird_thermal_type}/thermal.conf    ../alps/device/mediatek/mt6761/thermal.conf
		cp -a -T    driver/thermal_type/mt6761/${bird_thermal_type}/thermal.eng.conf    ../alps/device/mediatek/mt6761/thermal.eng.conf
	else	#bd_k62v1_64_bsp
		cp -a -T    driver/thermal_type/mt6765/${bird_thermal_type}/thermal.conf    ../alps/device/mediatek/mt6765/thermal.conf
		cp -a -T    driver/thermal_type/mt6765/${bird_thermal_type}/thermal.eng.conf    ../alps/device/mediatek/mt6765/thermal.eng.conf		
	fi	              
  fi   
      echo  "copy bird_thermal_type  H ${bird_thermal_type} OK"
fi


#copy bird_fmradio_cust
if [ -z ${bird_fmradio_cust} ]; then
  echo  "bird_fmradio_cust not config use default  = "
    cp -a -T    driver/fmradio_cust/fmradio_default/fm_cust.cfg    ../alps/vendor/mediatek/proprietary/hardware/connectivity/fmradio/config/mt6627/fm_cust.cfg
else    
  if [ -d driver/fmradio_cust/${bird_fmradio_cust} ]; then
    echo  "copy bird_fmradio_cust ${bird_fmradio_cust} OK"
    cp -a -T    driver/fmradio_cust/${bird_fmradio_cust}/fm_cust.cfg    ../alps/vendor/mediatek/proprietary/hardware/connectivity/fmradio/config/mt6627/fm_cust.cfg
  fi   
      
fi

echo "clone DRV end"

cd -
