package com.example.baapp.ui;

import android.content.Context;
import android.content.Intent;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.example.baapp.MainActivity;
import com.example.baapp.R;
import com.example.baapp.common.LanguageService;
import com.example.baapp.connection.SvConnectService;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MenuService {

    public static final int REQUEST_CODE_IMPORT_CSV = 1002;


    public static View.OnClickListener showPopupMenu(Context context, View anchorView) {
        return view -> {
            PopupMenu popupMenu = new PopupMenu(context, anchorView);
            MenuInflater inflater = popupMenu.getMenuInflater();
            inflater.inflate(R.menu.menu_main, popupMenu.getMenu());
            applyPopupMenuLanguage(context, popupMenu);

            popupMenu.setOnMenuItemClickListener(item -> {
                handleMenuItemClick(context, item);
                return true;
            });

            popupMenu.show();
        };
    }

    private static void handleMenuItemClick(Context context, MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.menu_item1) {
            if (context instanceof MainActivity) {
                DialogHelper.showLocationRegistrationDialog((MainActivity) context);
            } else {
                Toast.makeText(context, LanguageService.get(context).t("common.unknown_context"), Toast.LENGTH_SHORT).show();
            }
        } else if (itemId == R.id.menu_item2) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);

            if (context instanceof android.app.Activity) {
                ((android.app.Activity) context).startActivityForResult(
                        Intent.createChooser(
                                intent,
                                LanguageService.get(context).t("csv.select_file")
                        ), REQUEST_CODE_IMPORT_CSV
                );
            } else {
                Toast.makeText(context, LanguageService.get(context).t("csv.warn_import"), Toast.LENGTH_SHORT).show();
            }
        } else if (itemId == R.id.menu_item3) {
            SvConnectService.upload(context);
        } else if (itemId == R.id.menu_item4) {
            if (context instanceof MainActivity) {
                DialogHelper.showSearchConditionDialog((MainActivity) context);
            } else {
                Toast.makeText(context, LanguageService.get(context).t("common.unknown_context"), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(context, LanguageService.get(context).t("common.unknown_option"), Toast.LENGTH_SHORT).show();
        }
    }

    public static void setupBottomNavigation(MainActivity activity, BottomNavigationView bottomNavigation) {
        applyBottomNavigationLanguage(activity, bottomNavigation);
        bottomNavigation.setSelectedItemId(R.id.navigation_map);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.navigation_map) {
                activity.showMapMode();
                return true;
            } else if (itemId == R.id.navigation_timeline) {
                activity.showTimelineMode();
                return true;
            } else if (itemId == R.id.navigation_account) {
                activity.showAccountOptions();
                return true;
            }

            return false;
        });
    }

    private static void applyPopupMenuLanguage(Context context, PopupMenu popupMenu) {
        LanguageService language = LanguageService.get(context);
        language.setMenuTitle(popupMenu.getMenu(), R.id.menu_item1, "menu.register_location");
        language.setMenuTitle(popupMenu.getMenu(), R.id.menu_item2, "menu.csv_import");
        language.setMenuTitle(popupMenu.getMenu(), R.id.menu_item3, "menu.option3");
        language.setMenuTitle(popupMenu.getMenu(), R.id.menu_item4, "menu.search_condition");
    }

    private static void applyBottomNavigationLanguage(Context context, BottomNavigationView bottomNavigation) {
        LanguageService language = LanguageService.get(context);
        language.setMenuTitle(bottomNavigation.getMenu(), R.id.navigation_map, "bottom.map");
        language.setMenuTitle(bottomNavigation.getMenu(), R.id.navigation_timeline, "bottom.timeline");
        language.setMenuTitle(bottomNavigation.getMenu(), R.id.navigation_account, "bottom.account");
    }
}
