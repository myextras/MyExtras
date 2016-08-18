package au.com.myextras;

import android.content.Intent;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Date;

public class BulletinsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.bulletins_activity);

        recyclerView = (RecyclerView) findViewById(R.id.bulletins);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            final int spacing = getResources().getDimensionPixelSize(R.dimen.grid_spacing);

            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                int position = parent.getChildAdapterPosition(view);

                outRect.set(spacing, position == 0 ? spacing : 0, spacing, spacing);
            }
        });
        recyclerView.setAdapter(new BulletinAdapter());

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setSubtitle("The Hamilton and Alexandra College");
        }

        final View actionButton = findViewById(R.id.done_all);
        actionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                actionButton.setVisibility(View.GONE);
            }
        });

        startService(new Intent(this, SyncService.class));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.bulletins_activity, menu);

        return true;
    }

    private class BulletinViewHolder extends RecyclerView.ViewHolder {

        final TextView titleTextView;
        final TextView dateTextView;

        public BulletinViewHolder(View itemView) {
            super(itemView);

            titleTextView = (TextView) itemView.findViewById(R.id.title);
            dateTextView = (TextView) itemView.findViewById(R.id.date);
        }

    }

    private class BulletinAdapter extends RecyclerView.Adapter<BulletinViewHolder> {

        private Date[] items;

        public BulletinAdapter() {
            items = new Date[10];

            Calendar calendar = Calendar.getInstance();
            for (int index = 0; index < items.length; index++) {
                calendar.set(Calendar.HOUR, (int) (Math.random() * 23));
                calendar.set(Calendar.MINUTE, (int) (Math.random() * 59));
                items[index] = calendar.getTime();
                calendar.add(Calendar.DATE, -1);
            }
        }

        @Override
        public BulletinViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = getLayoutInflater().inflate(R.layout.bulletin_item, parent, false);
            return new BulletinViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(BulletinViewHolder holder, int position) {
            Date date = items[position];

            holder.titleTextView.setText(String.format("Extras for Wed %1$td/%1$tm/%1$tY", date));
            holder.titleTextView.setTypeface(position == 0 || position == 2 ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            holder.dateTextView.setText(String.format("Last update: %1$tF %1$tR", date));
        }

        @Override
        public int getItemCount() {
            return items.length;
        }

    }

}

