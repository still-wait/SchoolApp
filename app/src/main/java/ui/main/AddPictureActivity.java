package ui.main;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.schoolwq.BaseActivity;
import com.example.schoolwq.MainActivity;
import com.example.schoolwq.R;
import com.getbase.floatingactionbutton.FloatingActionsMenu;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static Internet.NormalPostRequest.mURL;


/**
 * Created by Y-GH on 2017/5/16.
 */
public class AddPictureActivity extends BaseActivity implements View.OnClickListener ,Runnable{

    private ImageView imageView;
    private TextView textView;
    private Button commit;
    private TextInputEditText desc;

    private Uri fileUri;
    private Uri cropUri;
    private static final int REQUEST_IMAGE_CAMERA = 100;
    private static final int REQUEST_IMAGE_SELECT = 200;
    private static final int RESULT_REQUEST_CODE = 300;
    public static final int MEDIA_TYPE_IMAGE = 1;
    private Bitmap bmp;
    private static final String LOG_TAG = "MainActivity";
    private ProgressDialog dialog;
    String imgPath;
    private String httpurl1 = mURL+":8080/wanqing/updateUserImage";
    private SharedPreferences pref;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.addpicture);
        initView();
    }

    @Override
    public void initToolbar() {
        super.initToolbar();
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
        toolbar.setNavigationIcon(R.mipmap.back1);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AddPictureActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void initView() {
        getSupportActionBar().setTitle("发布美景");
        imageView = (ImageView) findViewById(R.id.imageView);
        textView = (TextView) findViewById(R.id.textView1);
        desc = (TextInputEditText) findViewById(R.id.et_meijing);
        commit = (Button) findViewById(R.id.btn_commit1);
        commit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               /**
                * loading...
                */

                if (dialog == null) {
                    dialog = new ProgressDialog(AddPictureActivity.this);
                }
                dialog.setMessage("上传中...");
                dialog.setCancelable(false);
                dialog.show();
                new Thread(AddPictureActivity.this).start();
            }
        });

        final FloatingActionsMenu floatMenu  = (FloatingActionsMenu) findViewById(R.id.fab_menu);
        com.getbase.floatingactionbutton.FloatingActionButton camera = (com.getbase.floatingactionbutton.FloatingActionButton) findViewById(R.id.camera);
        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);
                Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                i.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);

                startActivityForResult(i, REQUEST_IMAGE_CAMERA);
                floatMenu.toggle();
            }
        });
        com.getbase.floatingactionbutton.FloatingActionButton select = (com.getbase.floatingactionbutton.FloatingActionButton) findViewById(R.id.select);
        select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, REQUEST_IMAGE_SELECT);
                floatMenu.toggle();
            }
        });

        desc.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {
                if (desc.getText().toString().length() > 0) {
                    if (imageView.getResources()!=null ) {
                        commit.setEnabled(true);
                    } else {
                        commit.setEnabled(false);
                    }
                } else {
                    commit.setEnabled(false);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {

            switch (requestCode){
                case REQUEST_IMAGE_CAMERA:
                    startPhotoZoom(fileUri);
                    break;
                case REQUEST_IMAGE_SELECT:
                    Uri selectedImage = data.getData();
                    startPhotoZoom(selectedImage);
                    break;
                case RESULT_REQUEST_CODE:
                    imgPath = cropUri.getPath();

                    bmp = BitmapFactory.decodeFile(imgPath);
                    imageView.setImageBitmap(bmp);
                    textView.setText("");
                    Log.e(LOG_TAG, imgPath);
                    Log.d(LOG_TAG, String.valueOf(bmp.getHeight()));
                    Log.d(LOG_TAG, String.valueOf(bmp.getWidth()));

                    break;
            }

        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void run() {
        try {
            PostFile(imgPath);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 上传文件
     * @param imgPath
     * @throws IOException
     * @throws JSONException
     */
    private void PostFile(String imgPath) throws IOException, JSONException {
        File file = new File(imgPath);
        if (!file.exists())
        {
            Toast.makeText(AddPictureActivity.this, "文件不存在，请修改文件路径", Toast.LENGTH_SHORT).show();
            return;
        }
        pref = getSharedPreferences("user", MODE_PRIVATE);
        String Username = pref.getString("username", "");
        RequestBody body = new  MultipartBody.Builder()
                .addFormDataPart("file_img",imgPath , RequestBody.create(MediaType.parse("media/type"), new File(imgPath)))
                .addFormDataPart("userid",Username)
                .addFormDataPart("imgdesc",desc.getText().toString())
                .build();

        Request request = new Request.Builder()
                .url(httpurl1)
                .post(body)
                .build();

        OkHttpClient client = new OkHttpClient();
        Response response = client.newCall(request).execute();
        if (response.isSuccessful()) {
            String tempResponse =  response.body().string();
            Log.e("==返回結果==","---"+tempResponse);
            if(Integer.parseInt(tempResponse)>0){
                mHandler.sendEmptyMessage(0);
            }else {
                Log.e("==返回==","---出错---");
                mHandler.sendEmptyMessage(1);
            }
        } else {
            dialog.cancel();
            mHandler.sendEmptyMessage(1);
            throw new IOException("Unexpected code " + response);
        }


    }


    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {

            switch (msg.what) {
                case 0:
                    dialog.cancel();
                    Toast.makeText(AddPictureActivity.this, "上传成功", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(AddPictureActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                    break;
                case 1:
                    dialog.cancel();
                    Toast.makeText(AddPictureActivity.this, "上传失败", Toast.LENGTH_SHORT).show();
                    break;
            }
        };
    };


    @Override
    public void onClick(View v) {

    }

    /**
     * Create a file Uri for saving an image or video
     */
    private static Uri getOutputMediaFileUri(int type) {
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /**
     * Create a File for saving an image or video
     */
    private static File getOutputMediaFile(int type) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "img_wanqing");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String name = "img_test";
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath()  + File.separator + name + ".jpg");
            Log.e("----文件路径----","====="+mediaFile);
        } else {
            return null;
        }

        return mediaFile;
    }

    /**
     * 裁剪图片
     */

    public void startPhotoZoom(Uri uri) {
        Intent intent = new Intent("com.android.camera.action.CROP");//发起剪切动作
        intent.setDataAndType(uri, "image/*");//设置剪切图片的uri和类型
        intent.putExtra("crop", "true");//剪切动作的信号
        intent.putExtra("aspectX", 1.6);//x和y是否等比缩放
        intent.putExtra("aspectY", 1);
        intent.putExtra("outputX", 500);
        intent.putExtra("outputY", 316);//剪切后图片的尺寸
        intent.putExtra("return-data", true);//是否把剪切后的图片通过data返回
        intent.putExtra("outputFormat", Bitmap.CompressFormat.PNG.toString());//图片的输出格式
        intent.putExtra("noFaceDetection", true);  //关闭面部识别
        //设置剪切的图片保存位置
        cropUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT,cropUri);
        startActivityForResult(intent, RESULT_REQUEST_CODE);
    }

}
