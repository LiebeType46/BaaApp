package com.example.baapp.ui;

import android.content.Context;
import android.content.Intent;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.example.baapp.R;

public class MenuService {

    public static final int REQUEST_CODE_IMPORT_CSV = 1002;


    public static View.OnClickListener showPopupMenu(Context context, View anchorView) {
        return view -> {
            PopupMenu popupMenu = new PopupMenu(context, anchorView);
            MenuInflater inflater = popupMenu.getMenuInflater();
            inflater.inflate(R.menu.menu_main, popupMenu.getMenu());

            popupMenu.setOnMenuItemClickListener(item -> {
                handleMenuItemClick(context, item);
                return true;
            });

            popupMenu.show();
        };
    }

    private static void handleMenuItemClick(Context context, MenuItem item) { // 戻り値を void に変更
        int itemId = item.getItemId();

        if (itemId == R.id.menu_item1) {
            DialogHelper.showLocationRegistrationDialog(context); // ダイアログを表示
        } else if (itemId == R.id.menu_item2) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);

            if (context instanceof android.app.Activity) {
                ((android.app.Activity) context).startActivityForResult(
                        Intent.createChooser(intent, context.getString(R.string.select_file)), REQUEST_CODE_IMPORT_CSV
                );
            } else {
                Toast.makeText(context, context.getString(R.string.warn_import), Toast.LENGTH_SHORT).show();
            }
        } else if (itemId == R.id.menu_item3) {
            Toast.makeText(context, "Option 3 Selected", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "Unknown Option", Toast.LENGTH_SHORT).show();
        }
    }
}
