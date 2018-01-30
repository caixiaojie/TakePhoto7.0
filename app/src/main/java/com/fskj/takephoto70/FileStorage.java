package com.fskj.takephoto70;

import android.os.Environment;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * author: Administrator
 * date: 2018/1/29 0029
 * desc:
 */

public class FileStorage {
    private File cropIconDir;
    private File iconDor;

    public FileStorage() {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            File external = Environment.getExternalStorageDirectory();
            String rootDir = "/" + "avater_path";//和刚在xml中的path保持一致
            cropIconDir = new File(external,rootDir+"/crop");
            if (!cropIconDir.exists()) {
                cropIconDir.mkdirs();
            }
            iconDor = new File(external,rootDir + "/icon");
            if (!iconDor.exists()) {
                iconDor.mkdirs();
            }
        }else {

        }
    }

    public File createGropFile() {
        String fileName = "";
        if (cropIconDir != null) {
            fileName = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".jpg";
        }
        return new File(cropIconDir,fileName);
    }
    public File createIconFile() {
        String fileName = "";
        if (iconDor != null) {
            fileName = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".jpg";
        }
        return new File(iconDor,fileName);
    }
}
