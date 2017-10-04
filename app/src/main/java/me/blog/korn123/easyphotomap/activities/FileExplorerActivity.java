package me.blog.korn123.easyphotomap.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.HorizontalScrollView;
import android.widget.ListView;
import android.widget.TextView;

import com.beardedhen.androidbootstrap.TypefaceProvider;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.blog.korn123.easyphotomap.R;
import me.blog.korn123.easyphotomap.adapters.ExplorerItemAdapter;
import me.blog.korn123.easyphotomap.constants.Constant;
import me.blog.korn123.easyphotomap.helper.RegistrationThread;
import me.blog.korn123.easyphotomap.models.FileItem;
import me.blog.korn123.easyphotomap.utils.CommonUtils;
import me.blog.korn123.easyphotomap.utils.DialogUtils;

/**
 * Created by CHO HANJOONG on 2016-07-16.
 */
public class FileExplorerActivity extends AppCompatActivity {

    private String mCurrent;
    private ArrayList<FileItem> mListFileItem;
    private ArrayList<FileItem> mListDirectoryEntity;
    private ViewGroup mViewGroup;
    private ProgressDialog mProgressDialog;
    private ArrayAdapter<FileItem> mAdapter;

    @BindView(R.id.filelist)
    public ListView mFileList;

    @BindView(R.id.scrollView)
    public HorizontalScrollView mScrollView;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TypefaceProvider.registerDefaultIconSets();
        setContentView(R.layout.activity_file_explorer);
        ButterKnife.bind(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.file_explorer_activity_title));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mListFileItem = new ArrayList<>();
        mListDirectoryEntity = new ArrayList<>();
        mCurrent = Constant.CAMERA_DIRECTORY;
        ((TextView)findViewById(R.id.registerDirectory)).setTypeface(Typeface.DEFAULT);

        mAdapter = new ExplorerItemAdapter(this, this, R.layout.item_file_explorer, this.mListFileItem);
        mFileList.setAdapter(mAdapter);
        mViewGroup = (ViewGroup)findViewById(R.id.pathView);
        mScrollView = (HorizontalScrollView)findViewById(R.id.scrollView);

        AdapterView.OnItemClickListener mItemClickListener =
                new AdapterView.OnItemClickListener() {
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        FileItem thumbnailEntity = (FileItem)parent.getAdapter().getItem(position);
                        String fileName = thumbnailEntity.getFileName();

                        if (fileName.startsWith("[") && fileName.endsWith("]")) {
                            fileName = fileName.substring(1, fileName.length()-1);
                        }

                        String path = mCurrent + "/" + fileName;
                        File f = new File(path);

                        if (f.isDirectory()) {
                            mCurrent = path;
                            refreshFiles();
                        } else {
                            if (!new File(Constant.WORKING_DIRECTORY).exists()) {
                                new File(Constant.WORKING_DIRECTORY).mkdirs();
                            }
                            PositiveListener positiveListener = new PositiveListener(FileExplorerActivity.this, FileExplorerActivity.this, FilenameUtils.getName(path) + ".origin", path);
                            DialogUtils.INSTANCE.showAlertDialog(FileExplorerActivity.this, getString(R.string.file_explorer_message7), FileExplorerActivity.this, path, positiveListener);
                        }
                    }
                };
        mFileList.setOnItemClickListener(mItemClickListener);
        refreshFiles();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @OnClick({R.id.registerDirectory})
    public void buttonClick(View view) {
        switch (view.getId()) {
            case R.id.registerDirectory:
                if (mListFileItem.size() - mListDirectoryEntity.size() < 1) {
                    DialogUtils.INSTANCE.showAlertDialog(this, getString(R.string.file_explorer_message9));
                } else {
                    PositiveListener positiveListener = new PositiveListener(FileExplorerActivity.this, FileExplorerActivity.this, null, null);
                    DialogUtils.INSTANCE.showAlertDialog(FileExplorerActivity.this, getString(R.string.file_explorer_message11) , FileExplorerActivity.this, positiveListener);
                }
                break;
        }
    }

    void refreshFiles() {
        String[] arrayPath = StringUtils.split(mCurrent, "/");
        mViewGroup.removeViews(0, mViewGroup.getChildCount());
        String currentPath = "";
        int index = 0;
        for (String path : arrayPath) {
            currentPath += ("/" + path);
            final String targetPath = currentPath;
            TextView textView = new TextView(this);
            if(index < arrayPath.length - 1) {
                textView.setText(path + "  >  ");
            } else {
                textView.setText(path);
            }

            if (StringUtils.equals(arrayPath[arrayPath.length - 1], path)) {
                textView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                textView.setTextColor(ContextCompat.getColor(FileExplorerActivity.this, R.color.colorPrimary));
            } else {
                textView.setTypeface(Typeface.DEFAULT);
                textView.setTextColor(ContextCompat.getColor(FileExplorerActivity.this, R.color.defaultFont));
            }
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            textView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
            textView.setGravity(Gravity.CENTER_VERTICAL);
            textView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mCurrent = targetPath;
                    refreshFiles();
                }
            });
            mViewGroup.addView(textView);
            index++;
        }
        mScrollView.postDelayed(new Runnable() {
            public void run() {
                mScrollView.fullScroll(HorizontalScrollView.FOCUS_RIGHT);
            }
        }, 100L);
        new RefreshThread().start();
    }

    @Override
    public void onBackPressed() {
        DialogUtils.INSTANCE.showAlertDialog(FileExplorerActivity.this, getString(R.string.file_explorer_message12), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
            }
        }, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
    }

    public class PositiveListener {
        Context context;
        Activity activity;
        String fileName;
        String path;

        PositiveListener(Context context, Activity activity, String fileName, String path) {
            this.context = context;
            this.activity = activity;
            this.fileName = fileName;
            this.path = path;
        }

        public void register() {
            if (fileName != null && path != null) {
                mProgressDialog = ProgressDialog.show(FileExplorerActivity.this, getString(R.string.file_explorer_message5), getString(R.string.file_explorer_message6));
                Thread registerThread = new RegistrationThread(context, activity, mProgressDialog, fileName, path);
                registerThread.start();
            } else {
                Intent batchIntent = new Intent(FileExplorerActivity.this, BatchPopupActivity.class);
                ArrayList<String> listImagePath = new ArrayList<>();
                for (int i = mListDirectoryEntity.size(); i < mListFileItem.size(); i++) {
                    listImagePath.add(mListFileItem.get(i).getImagePath());
                }
                batchIntent.putStringArrayListExtra("listImagePath", listImagePath);
                startActivity(batchIntent);
            }
        }
    }

    class RefreshThread extends Thread {
        @Override
        public void run() {
            mListFileItem.clear();
            mListDirectoryEntity.clear();
            File current = new File(FileExplorerActivity.this.mCurrent);
            String[] files = current.list();
            if (files != null) {
                for (int i = 0; i < files.length;i++) {
                    FileItem thumbnailEntity = new FileItem();
                    String path = FileExplorerActivity.this.mCurrent + "/" + files[i];
                    String name = "";
                    File f = new File(path);
                    if (f.isDirectory()) {
                        name = "[" + files[i] + "]";
                        thumbnailEntity.setImagePath(name);
                        thumbnailEntity.isDirectory = true;
                        mListDirectoryEntity.add(thumbnailEntity);
                    } else {
                        name = files[i];
                        String extension = FilenameUtils.getExtension(name).toLowerCase();
                        if (!extension.matches("jpg|jpeg")) continue;
                        thumbnailEntity.setImagePath(path);
                        mListFileItem.add(thumbnailEntity);
                    }
                }
            }

            if (CommonUtils.loadBooleanPreference(FileExplorerActivity.this, "enable_reverse_order")) {
                Collections.sort(mListDirectoryEntity, Collections.reverseOrder());
                Collections.sort(mListFileItem, Collections.reverseOrder());
            } else {
                Collections.sort(mListDirectoryEntity);
                Collections.sort(mListFileItem);
            }
            mListFileItem.addAll(0, mListDirectoryEntity);

            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    mAdapter.notifyDataSetChanged();
                    mFileList.setSelection(0);
                }
            });
        }
    }

}
