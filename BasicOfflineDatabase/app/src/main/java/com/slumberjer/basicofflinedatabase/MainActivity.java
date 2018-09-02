package com.slumberjer.basicofflinedatabase;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    SQLiteDatabase db1 = null;
    EditText edname,edmatric,edcourse;
    private ArrayList<HashMap<String, String>> studentlist;
    private ListView listViewStudent;
    private ImageView imageView;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    String mCurrentPhotoPath;
    Button btnnew;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        edmatric = (EditText) findViewById(R.id.edMatric);
        edname = (EditText) findViewById(R.id.edName);
        edcourse = (EditText) findViewById(R.id.edCourse);
        btnnew = (Button) findViewById(R.id.btnNew);
        btnnew.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearAll();
            }
        });
        listViewStudent = (ListView)findViewById(R.id.listStudent);
        imageView = (ImageView) findViewById(R.id.imageView);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                }
            }
        });
        listViewStudent.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String item = listViewStudent.getItemAtPosition(position).toString();
                String[] userarray = item.split("\\s*,\\s*");
                edmatric.setText(userarray[1].replace("matric=",""));
                edname.setText(userarray[2].replace("name=",""));
                edcourse.setText((userarray[3].replace("course=","").replace("}","")));
                loadImage(userarray[1].replace("matric=",""));
            }
        });
        studentlist = new ArrayList<>();
        createdb();
        loadalllist();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            imageView.setImageBitmap(imageBitmap);
            imageView.buildDrawingCache();
        }
    }

    public void createdb(){
        db1 = this.openOrCreateDatabase("dbstudent",MODE_PRIVATE,null);
        String sqlcreate = "CREATE TABLE IF NOT EXISTS student" +
                "(MATRIC VARCHAR NOT NULL,NAME VARCHAR, COURSE VARCHAR, PRIMARY KEY (MATRIC));";
        db1.execSQL(sqlcreate);
    }

    public void insertdata(View v) {
        String name = edname.getText().toString();
        String matric = edmatric.getText().toString();
        String course = edcourse.getText().toString();
        if (TextUtils.isEmpty(name)){
            edname.setError("Input name");
            return;
        }
        if (TextUtils.isEmpty(matric)){
            edmatric.setError("Input matric");
            return;
        }
        if (TextUtils.isEmpty(course)){
            edcourse.setError("Input course");
            return;
        }

        if (imageView.getDrawable() == null){
            Toast.makeText(this, "NO PICTURE TAKEN. CLICK ON THE BLUE BOX TO TAKE PICTURE.", Toast.LENGTH_LONG).show();
            return;
        }

        try{
            String sqlinsert = "INSERT INTO student(MATRIC,NAME,COURSE) VALUES('"+matric+"','"+name+"','"+course+"');";
            db1.execSQL(sqlinsert);
            Toast.makeText(this, "INSERT DATA SUCCESSFULL", Toast.LENGTH_SHORT).show();
            saveImage(matric);
            loadalllist();
            clearAll();
        }catch (Exception e){
            Toast.makeText(this, "DUPLICATE MATRIC", Toast.LENGTH_SHORT).show();

            edmatric.setError("DUPLICATE!!!");
            loadalllist();
        }

    }

    public void deletedata(View v){
        String matric = edmatric.getText().toString();
        String sqlsearch = "SELECT * FROM student WHERE MATRIC = '"+matric+"'";
        Cursor c = db1.rawQuery(sqlsearch,null);
        if (c.getCount()> 0){
            try{
                String sqldelete = "DELETE FROM student WHERE MATRIC = '"+matric+"'";
                db1.execSQL(sqldelete);
                Toast.makeText(this, "DELETE DATA SUCCESSFULL", Toast.LENGTH_SHORT).show();
                deleteImage(matric);
                loadalllist();
                clearAll();
            }catch (Exception e){
                loadalllist();
                Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
            }
        }else{
            Toast.makeText(this, "MATRIC NOT FOUND", Toast.LENGTH_SHORT).show();
            loadalllist();
        }
    }

    public void updatedate(View v){
        String matric = edmatric.getText().toString();
        String name = edname.getText().toString();
        String course = edcourse.getText().toString();
        String sqlsearch = "SELECT * FROM student WHERE MATRIC = '"+matric+"'";
        Cursor c = db1.rawQuery(sqlsearch,null);
        if (c.getCount()> 0){
            String sqlupdate = "UPDATE student SET NAME = '"+name+"' , COURSE = '"+course+"' WHERE MATRIC = '"+matric+"'";
            db1.execSQL(sqlupdate);
            Toast.makeText(this, "UPDATE SUCCESSFULL", Toast.LENGTH_SHORT).show();
            deleteImage(matric);
            saveImage(matric);
            loadalllist();
        }else{
            loadalllist();
            Toast.makeText(this, "MATRIC NOT FOUND", Toast.LENGTH_SHORT).show();
        }
    }

    public void searcdata(String matric){
        String sqlsearch = "SELECT * FROM student WHERE MATRIC = '"+matric+"'";
        Cursor c = db1.rawQuery(sqlsearch,null);
        if (c.getCount()> 0){
            c.moveToFirst();
            studentlist.clear();
            String dbmatric =  c.getString(c.getColumnIndex("MATRIC"));
            String dbname = c.getString(c.getColumnIndex("NAME"));
            String dbcourse = c.getString(c.getColumnIndex("COURSE"));
            HashMap<String, String> studentls = new HashMap<>(); //temporary hashmap arrary
            studentls.put("image",imageloc(dbmatric));
            studentls.put("name",dbname);
            studentls.put("matric",dbmatric);
            studentls.put("course",dbcourse);
            studentlist.add(studentls);//add to array
            ListAdapter adapter = new SimpleAdapter( //setup custom adapter for listview
                    MainActivity.this, studentlist,//custom interface using listactivity.xml
                    R.layout.customlist, new String[]{"image","name","matric","course"}, new int[]{R.id.imageView2,R.id.textName,R.id.textMatric,R.id.textCourse});
            listViewStudent.setAdapter(adapter);
            Toast.makeText(this, "DATA FOUND!", Toast.LENGTH_SHORT).show();
            return;
        }else{
            loadalllist();
            Toast.makeText(this, "DATA NOT FOUND", Toast.LENGTH_SHORT).show();
        }
    }

    public void loadalllist(){
        String sqlsearch = "SELECT * FROM student";
        Cursor c = db1.rawQuery(sqlsearch,null);
        studentlist.clear();
        if (c.getCount()>0){
            c.moveToFirst();
            for (int i =0;i<c.getCount();i++){
                String dbmatric =  c.getString(c.getColumnIndex("MATRIC"));
                String dbname = c.getString(c.getColumnIndex("NAME"));
                String dbcourse = c.getString(c.getColumnIndex("COURSE"));
                HashMap<String, String> studentls = new HashMap<>(); //temporary hashmap arrary
                studentls.put("image",imageloc(dbmatric));
                studentls.put("name",dbname);
                studentls.put("matric",dbmatric);
                studentls.put("course",dbcourse);
                studentlist.add(studentls);//add to array
                c.moveToNext();
            }
            ListAdapter adapter = new SimpleAdapter( //setup custom adapter for listview
                    MainActivity.this, studentlist,//custom interface using listactivity.xml
                    R.layout.customlist, new String[]{"image","name","matric","course"}, new int[]{R.id.imageView2,R.id.textName,R.id.textMatric,R.id.textCourse});
            listViewStudent.setAdapter(adapter);
        }else{
            listViewStudent.setAdapter(null);
            Toast.makeText(this, "NO DATA", Toast.LENGTH_SHORT).show();
        }
    }



    public void saveImage(String matric) {
        // Create an image file name
        BitmapDrawable drawable = (BitmapDrawable) imageView.getDrawable();
        Bitmap bitmap = drawable.getBitmap();
        ContextWrapper cw = new ContextWrapper(getApplicationContext());

        File directory = cw.getDir("basic", Context.MODE_PRIVATE);
        if (!directory.exists()) {
            directory.mkdir();
        }
        File mypath = new File(directory, "/"+matric+".jpg");
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mypath);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
        } catch (Exception e) {
            Toast.makeText(cw,e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    public String imageloc(String matric){
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        File directory = cw.getDir("basic", Context.MODE_PRIVATE);
        File myimagepath = new File(directory, "/"+matric+".jpg");
        return myimagepath.toString();
    }

    public void loadImage(String matric){
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        File directory = cw.getDir("basic", Context.MODE_PRIVATE);
        File myimagepath = new File(directory, "/"+matric+".jpg");
        if(myimagepath.exists()){
            Bitmap myBitmap = BitmapFactory.decodeFile(myimagepath.getAbsolutePath());
            imageView.setImageBitmap(myBitmap);
        }
    }

    public void deleteImage(String matric){
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        File directory = cw.getDir("basic", Context.MODE_PRIVATE);
        File myimagepath = new File(directory, "/"+matric+".jpg");
        if (myimagepath.exists()){
            myimagepath.delete();
        }
    }

    public void clearAll(){
        edcourse.setText("");
        edmatric.setText("");
        edname.setText("");
        imageView.setImageBitmap(null);
    }

    public void promptMatric(View v){
        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.prompt_layout, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                this);
        alertDialogBuilder.setView(promptsView);
        final EditText userInput = (EditText) promptsView
                .findViewById(R.id.editTextDialogUserInput);
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                searcdata(userInput.getText().toString());
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                loadalllist();
                                dialog.cancel();
                            }
                        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
}

