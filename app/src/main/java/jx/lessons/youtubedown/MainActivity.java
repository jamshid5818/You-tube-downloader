package jx.lessons.youtubedown;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import at.huber.youtubeExtractor.VideoMeta;
import at.huber.youtubeExtractor.YouTubeExtractor;
import at.huber.youtubeExtractor.YtFile;

public class MainActivity extends AppCompatActivity {
    Button button;
    Button paste;
    EditText editText;
    private static String youtubeLink;
    private LinearLayout mainLayout;
    private ProgressBar mainProgressBar;
    DownloadHelper downloadHelper;
    ImageView imageView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        downloadHelper = new DownloadHelper(this);
        button = findViewById(R.id.button1);
        paste = findViewById(R.id.button2);
        editText = findViewById(R.id.link);
        mainLayout = findViewById(R.id.main_layout);
        mainProgressBar = findViewById(R.id.prgrBarMain);
        imageView = findViewById(R.id.image_video);
        mainProgressBar.setVisibility(View.GONE);
        button.setOnClickListener(view -> {
            mainProgressBar.setVisibility(View.VISIBLE);
            permissions();
            // Check how it was started and if we can get the youtube link
                String ytLink = editText.getText().toString();

                if (ytLink.contains("://youtu.be/") || ytLink.contains("youtube.com/watch?v=")) {
                    youtubeLink = ytLink;
                    // We have a valid link
                    imageView.setVisibility(View.VISIBLE);
                    imageView.setImageResource(R.drawable.img_1);
                    getYoutubeDownloadUrl(youtubeLink);
                } else {
                    mainProgressBar.setVisibility(View.INVISIBLE);
                    Toast.makeText(this, R.string.error_no_yt_link, Toast.LENGTH_LONG).show();
                }
        });
        paste.setOnClickListener(view -> {
//            pasteData();
            editText.setText(downloadHelper.pasteData());
        });
    }
    @SuppressLint("StaticFieldLeak")
    public void getYoutubeDownloadUrl(String youtubeLink) {
        new YouTubeExtractor(this) {

            @SuppressLint("StaticFieldLeak")
            @Override
            public void onExtractionComplete(SparseArray<YtFile> ytFiles, VideoMeta vMeta) {
                mainProgressBar.setVisibility(View.GONE);

                if (ytFiles == null) {
                    // Something went wrong we got no urls. Always check this.
                    return;
                }
                // Iterate over itags
//                Picasso.get().load(vMeta.getThumbUrl()).into(imageView);
                Picasso.get().load(vMeta.getHqImageUrl()).into(imageView);

                Log.d("THUMB", vMeta.getThumbUrl());
                imageView.setOnClickListener(view -> {
                    Uri uri = Uri.parse(youtubeLink); // missing 'http://' will cause crashed
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                });
                for (int i = 0, itag; i < ytFiles.size(); i++) {
                    itag = ytFiles.keyAt(i);
                    // ytFile represents one file with its url and meta data
                    YtFile ytFile = ytFiles.get(itag);
                    // Just add videos in a decent format => height -1 = audio
                    if (ytFile.getFormat().getHeight() == -1 || ytFile.getFormat().getHeight() >= 360) {
                        addButtonToMainLayout(vMeta.getTitle(), ytFile, vMeta.getViewCount());

                    }
                }
            }
        }.extract(youtubeLink);
    }

    private void addButtonToMainLayout(final String videoTitle, final YtFile ytfile, Long hajmi) {
        // Display some buttons and let the user choose the format

        String btnText = (ytfile.getFormat().getHeight() == -1) ? "Audio " +
                ytfile.getFormat().getAudioBitrate() + " kbit/s" :
                ytfile.getFormat().getHeight() + "p";
        Button btn = new Button(this);
        btn.setWidth(90);
        btn.setHeight(21);
        btn.setMarqueeRepeatLimit(15);
        btn.setText(btnText);
        btn.setBackgroundResource(R.drawable.bacground_create_buttons);
        btn.setOnClickListener(v -> {
            String filename;
            if (videoTitle.length() > 55) {
                filename = videoTitle.substring(0, 55) + "." + ytfile.getFormat().getExt();
            } else {
                filename = videoTitle + "." + ytfile.getFormat().getExt();
            }
            filename = filename.replaceAll("[\\\\><\"|*?%:#/]", "");
            downloadHelper.downloadFromUrl(ytfile.getUrl(), videoTitle, filename);
            finish();
        });
        mainLayout.addView(btn);
    }
    public void permissions(){
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
            if (ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},100);

            }
        }
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.R){
            if (!Environment.isExternalStorageManager()){
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.addCategory("android.intent.category.DEFAULT");
                    intent.setData(Uri.parse(String.format("package:%s",this.getPackageName())));
                    startActivityIfNeeded(intent,101);
                }catch (Exception e){
                    Log.d("EXCEPTION PERMISSION", e.getMessage());
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    startActivity(intent);
                }
            }
        }
    }

}