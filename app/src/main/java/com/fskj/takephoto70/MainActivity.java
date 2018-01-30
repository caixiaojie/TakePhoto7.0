package com.fskj.takephoto70;

import android.Manifest;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PICK_IMAGE = 2;
    private static final int REQUEST_CAPTURE = 3;
    private static final String AUTHOR_PROVIDERS = "com.fskj.takephoto70.fileprovider";
    private static final int REQUEST_PICK_CUT = 4;
    private ImageView imgPhoto;
    private Activity activity;
    private Uri imageUri;
    private Uri outputUri;
    private String imagePath;
    private File file;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        activity = MainActivity.this;
        imgPhoto = (ImageView) findViewById(R.id.img_photo);
        /**
         * 授权
         */
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},5);
        }
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},6);
        }
    }

    public void selectImg(View view) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
        }else {
            selectPickture();
        }
    }
    public void takePhoto(View view) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
        }else {
            openCamara();
        }
    }

    /**
     * 选择照片
     */
    private void selectPickture() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,"image/*");
        startActivityForResult(intent,REQUEST_PICK_IMAGE);
    }



    /**
     * 打开相机
     */
    private void openCamara() {
        file = new FileStorage().createIconFile();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                imageUri = FileProvider.getUriForFile(activity,AUTHOR_PROVIDERS, file);
            }catch (Exception e) {
                e.printStackTrace();
                imageUri = Uri.fromFile(file);
            }
        }
        Intent intent = new Intent();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);//适配7.0
        }
        intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri);
        startActivityForResult(intent,REQUEST_CAPTURE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_IMAGE) {//选择相册并裁剪后的回调
            if (resultCode == RESULT_CANCELED) {
                return;
            }
            if (Build.VERSION.SDK_INT >= 19) {
                handleImageOnKitKat(data);//7.0
            }else {
                handleImageBeforeKitKat(data);
            }
        }

        if (requestCode == REQUEST_PICK_CUT) {//裁剪后
            Bitmap bitmap = null;
            try {
                bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
                imgPhoto.setImageBitmap(bitmap);
            }catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (requestCode == REQUEST_CAPTURE) {//拍照后返回
            if (resultCode == RESULT_CANCELED) {
                return;
            }
            if (resultCode == RESULT_OK) {
                cropPhoto(imageUri);
            }
        }
    }

    private void handleImageBeforeKitKat(Intent data) {
        imageUri = data.getData();
        imagePath = getImagePath(imageUri,null);
        cropPhoto(imageUri);
    }

    private void handleImageOnKitKat(Intent data) {
        imagePath = null;
        imageUri = data.getData();
        if (DocumentsContract.isDocumentUri(activity,imageUri)) {
            String docId = DocumentsContract.getDocumentId(imageUri);
            if ("com.android.providers.media.documents".equals(imageUri.getAuthority())) {
                String id = docId.split(":")[1];
                String selection = MediaStore.Images.Media._ID + "=" + id;
                imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,selection);
            }else if ("com.android.downloads.documents".equals(imageUri.getAuthority())) {
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"),Long.valueOf(docId));
                imagePath = getImagePath(contentUri,null);
            }
        }else if ("content".equalsIgnoreCase(imageUri.getScheme())) {
            imagePath = getImagePath(imageUri,null);
        }else if ("file".equalsIgnoreCase(imageUri.getScheme())) {
            imagePath = imageUri.getPath();
        }
        cropPhoto(imageUri);
    }

    /**
     * 获取文件路径
     * @param uri
     * @param selection
     * @return
     */
    private String getImagePath(Uri uri, String selection) {
        String path = null;
        Cursor cursor = getContentResolver().query(uri,null,selection,null,null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }

    /**
     * 裁剪图片
     */
    private void cropPhoto(Uri uri) {
//        File file = new FileStorage().createGropFile();
//        outputUri = Uri.fromFile(file);
        if (uri == null) {
            Log.e("uri","uri == null");
            return;
        }
        Intent intent = new Intent("com.android.camera.action.CROP");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        intent.setDataAndType(imageUri,"image/*");
        intent.putExtra("crop","true");
        intent.putExtra("aspectX",1);
        intent.putExtra("aspectY",1);
        intent.putExtra("scale",true);
        intent.putExtra("return-data",false);
        intent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri);
        intent.putExtra("outputFormat",Bitmap.CompressFormat.JPEG.toString());
        intent.putExtra("noFaceDetection",true);
        intent.putExtra("outputX",96);//指定裁剪后的图片大小，避免出现大图片上传失败
        intent.putExtra("outputY",96);
        startActivityForResult(intent,REQUEST_PICK_CUT);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
