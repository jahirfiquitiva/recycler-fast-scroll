package com.pluscubed.recyclerfastscrollsample;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.snackbar.Snackbar;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.pluscubed.recyclerfastscroll.RecyclerFastScroller;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class CoordinatorScrollingActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_coordinator_scrolling);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        RecyclerView view = findViewById(R.id.recyclerview);
        view.setAdapter(new ItemAdapter());
        view.setLayoutManager(new LinearLayoutManager(this));
        
        RecyclerFastScroller scroller = findViewById(R.id.fast_scroller);
        scroller.attachRecyclerView(view);
        CoordinatorLayout coordinatorLayout = findViewById(R.id.coordinator);
        AppBarLayout appBarLayout = findViewById(R.id.app_bar);
        scroller.attachAppBarLayout(coordinatorLayout, appBarLayout);
        
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    /*@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_scrolling, menu);
        return true;
    }*/
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ViewHolder> {
        ItemAdapter() {
        }
        
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(CoordinatorScrollingActivity.this)
                .inflate(R.layout.list_item, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.textView.setText(String.format(getString(R.string.item_number), position + 1));
        }
        
        @Override
        public int getItemCount() {
            return 40;
        }
        
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textView;
            
            public ViewHolder(View itemView) {
                super(itemView);
                
                textView = itemView.findViewById(R.id.list_item_text);
                
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Snackbar.make(v, String
                                          .format(getString(R.string.item_pressed_snackbar),
                                                  textView.getText()),
                                      Snackbar.LENGTH_SHORT)
                            .show();
                    }
                });
            }
        }
    }
}
