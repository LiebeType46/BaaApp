package com.example.baapp.photo;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import androidx.exifinterface.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.ImageView;

import androidx.core.content.FileProvider;

import com.example.baapp.R;
import com.example.baapp.common.ConstCode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PhotoService {

    private static PhotoService instance;
    private final Context context;
    private OnPhotoSelectedListener callback;

    private PhotoService(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized PhotoService getInstance(Context context) {
        if (instance == null) {
            instance = new PhotoService(context);
        }
        return instance;
    }

    public interface OnPhotoSelectedListener {
        void onPhotoSelected(Uri uri);
    }

    // 写真撮影用Intentを返す
    public void launchCamera(Activity activity, PhotoCaptureCallback callback) throws IOException {
        // 一時ファイルの作成
        File photoFile = createImageFile();
        Uri photoUri = FileProvider.getUriForFile(
                context,
                context.getPackageName() + ".fileprovider",
                photoFile
        );

        // 保持しておく（Activityに通知する必要があるため）
        PhotoSession.getInstance().setPhotoUri(photoUri);
        PhotoSession.getInstance().setCallback(callback);

        // カメラ起動
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
        activity.startActivityForResult(intent, ConstCode.REQUEST_TAKE_PHOTO);
    }

    // 一時ファイル作成
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName = "JPEG_" + timeStamp + "_";
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(fileName, ".jpg", storageDir);
    }

    public File copyAndResizePhotoFromUri(Uri uri) throws IOException {
        InputStream input = context.getContentResolver().openInputStream(uri);
        Bitmap original = BitmapFactory.decodeStream(input);

        if (input != null) {
            input.close();
        }

        // 向き補正
        InputStream exifInput = context.getContentResolver().openInputStream(uri);
        ExifInterface exif = new ExifInterface(exifInput);
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        Bitmap rotated = rotateBitmapIfRequired(original, orientation);

        if (exifInput != null) {
            exifInput.close();
        }


        // サイズ調整（長辺を1024に）
        Bitmap resized = resizeBitmap(rotated, 1024);

        // 保存先ファイル生成
        String fileName = "IMG_" + System.currentTimeMillis() + ".jpg";
        File photoFile = new File(context.getFilesDir(), "photos");
        if (!photoFile.exists()){
            photoFile.mkdirs();
        }

        File output = new File(photoFile, fileName);
        FileOutputStream out = new FileOutputStream(output);
        resized.compress(Bitmap.CompressFormat.JPEG, 85, out);
        rotated.recycle();
        original.recycle();
        out.close();

        return output;
    }

    private Bitmap resizeBitmap(Bitmap source, int maxLength) {
        int width = source.getWidth();
        int height = source.getHeight();
        float scale = (float) maxLength / Math.max(width, height);
        if (scale < 1.0f) {
            return Bitmap.createScaledBitmap(source,
                    Math.round(width * scale), Math.round(height * scale), true);
        }
        return source;
    }

    public Bitmap generateThumbnail(File imageFile) {
        Bitmap original = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
        return resizeBitmap(original, 256); // 長辺256px
    }

    private Bitmap rotateBitmapIfRequired(Bitmap bitmap, int orientation) {
        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270);
                break;
            default:
                return bitmap; // 補正不要
        }

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public static void showFullSizePhoto(Context context, String uriStr) {
        Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_full_photo);

        ImageView imageView = dialog.findViewById(R.id.full_image);
        imageView.setImageURI(Uri.parse(uriStr));

        dialog.show();
    }

    public void launchGallery(Activity activity, PhotoCaptureCallback callback) {
        PhotoSession.getInstance().setCallback(callback);

        // ギャラリーを開くインテント
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        activity.startActivityForResult(intent, ConstCode.REQUEST_GALLERY);
    }


}
