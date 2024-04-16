package com.psl.pallettracking;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.psl.pallettracking.adapters.AssetTypeSearchAdapter;
import com.psl.pallettracking.adapters.AutocompleteCustomArrayAdapter;
import com.psl.pallettracking.adapters.MyObject;
import com.psl.pallettracking.database.DatabaseHandler;
import com.psl.pallettracking.databinding.ActivityAssetSearchBinding;
import com.psl.pallettracking.helper.AppConstants;
import com.psl.pallettracking.helper.AssetUtils;
import com.psl.pallettracking.helper.CustomAutoCompleteTextChangedListener;
import com.psl.pallettracking.helper.SharedPreferencesManager;
import com.psl.pallettracking.rfid.RFIDInterface;
import com.psl.pallettracking.rfid.SeuicGlobalRfidHandler;
import com.seuic.uhf.EPC;
import com.seuic.uhf.UHFService;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import gr.escsoft.michaelprimez.searchablespinner.SearchableSpinner;
import gr.escsoft.michaelprimez.searchablespinner.interfaces.IStatusListener;
import gr.escsoft.michaelprimez.searchablespinner.interfaces.OnItemSelectedListener;

import static com.psl.pallettracking.helper.BaseUtil.getHexByteArray;

public class AssetSearchActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    public ActivityAssetSearchBinding binding;
    private Context context = this;
    private SeuicGlobalRfidHandler rfidHandler;
    ArrayList<String> assetTypeList = new ArrayList<>();

    private Timer beepTimer;
    private Timer startStopTimer;
    private int inv_period = 0;

    private SearchableSpinner spAssetType;
    private AssetTypeSearchAdapter searchAdapter;
    private String SELECTED_ASSET_TYPE_ITEM = "";
    private String SELECTED_ASSET_TYPE_NAME = "";
    public String SELECTED_ASSET_TYPE_ID = "";
    private int SELECTED_ASSET_POSITION = 0;
    private boolean isSearchOn = false;

    private Thread mThread;
    private int maximum_value = 0;
    private int total_assets_found = 0;
    public DatabaseHandler db;
    public ArrayAdapter<MyObject> myAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_asset_search);
        getSupportActionBar().hide();
        setTitle("ASSET SEARCH");
        db = new DatabaseHandler(context);
        binding.myAutoComplete.setText("");

        assetTypeList = db.getAllAssetTypeNamesForSearchSpinner();
        binding.myAutoComplete.addTextChangedListener(new CustomAutoCompleteTextChangedListener(AssetSearchActivity.this));


        if (db.getAssetMasterCount() > 0) {
            binding.myAutoComplete.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, View arg1, int pos, long id) {


                    RelativeLayout rl = (RelativeLayout) arg1;
                    TextView tv = (TextView) rl.getChildAt(0);
                    TextView tv1 = (TextView) rl.getChildAt(1);
                    // text.setText(tv1.getText().toString());
                    binding.myAutoComplete.setText(tv.getText().toString());
                }

            });
            // ObjectItemData has no value at first
            MyObject[] ObjectItemData = new MyObject[0];

            // set the custom ArrayAdapter
            myAdapter = new AutocompleteCustomArrayAdapter(AssetSearchActivity.this, R.layout.list_view_row_item, ObjectItemData);
            binding.myAutoComplete.setAdapter(myAdapter);

        }


        notifyAdapter();
        beepTimer = new Timer();
       /* if (startStopTimer != null) {
            startStopTimer.cancel();
        }
        startStopTimer = new Timer();*/
        beepTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                //Called each time when 1000 milliseconds (1 second) (the period parameter)
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isSearchOn) {
                           // if (maximum_value > 0) {
                                inv_period++;
                                disableSpinner();
                                binding.textPercentage.setText("" + maximum_value + " %");
                                binding.textPercentage.setBackgroundDrawable(getResources().getDrawable(R.drawable.round_button_red));
                                binding.textTotal.setText("" + total_assets_found);
                                rfidHandler.playSound();
                                maximum_value = 0;
                                total_assets_found = 0;
                                if (inv_period > 4) {
                                    inv_period = 0;
                                    if (isSearchOn) {
                                        rfidHandler.stopInventory();
                                        new Handler().postDelayed(() -> {
                                            isSearchOn = true;
                                            disableSpinner();
                                            rfidHandler.startSearchInventory(SharedPreferencesManager.getCompanyCode(context) + SELECTED_ASSET_TYPE_ID);
                                            //rfidHandler.startInventory();
                                            binding.btnSearch.setText("Stop");
                                            binding.btnSearch.setBackgroundDrawable(getResources().getDrawable(R.drawable.round_button_red));
                                        }, 150);

                                    } else {
                                        enableSpinner();
                                        isSearchOn = false;
                                        rfidHandler.stopInventory();
                                        binding.btnSearch.setText("Start");
                                        binding.btnSearch.setBackgroundDrawable(getResources().getDrawable(R.drawable.round_button_green));

                                    }
                                }
                           // }

                        } else {
                            enableSpinner();
                            isSearchOn = false;
                            binding.btnSearch.setText("Start");
                            binding.btnSearch.setBackgroundDrawable(getResources().getDrawable(R.drawable.round_button_green));
                            binding.textPercentage.setText("0 %");
                            binding.textPercentage.setBackgroundDrawable(getResources().getDrawable(R.drawable.round_button_green));
                            binding.textTotal.setText("0");
                        }
                    }
                });

            }

        }, 0, 1000);

       /* startStopTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                //Called each time when 1000 milliseconds (1 second) (the period parameter)
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isSearchOn) {
                            rfidHandler.stopInventory();
                            new Handler().postDelayed(() -> {
                                isSearchOn = true;
                                disableSpinner();
                                rfidHandler.startSearchInventory(SharedPreferencesManager.getCompanyCode(context) + SELECTED_ASSET_TYPE_ID);
                                //rfidHandler.startInventory();
                                binding.btnSearch.setText("Stop");
                                binding.btnSearch.setBackgroundDrawable(getResources().getDrawable(R.drawable.round_button_red));
                            }, 100);

                        } else {
                            enableSpinner();
                            isSearchOn = false;
                            rfidHandler.stopInventory();
                            binding.btnSearch.setText("Start");
                            binding.btnSearch.setBackgroundDrawable(getResources().getDrawable(R.drawable.round_button_green));

                        }
                    }
                });

            }

        }, 0, 5500);*/


        binding.btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (SELECTED_ASSET_TYPE_ITEM.equalsIgnoreCase("")) {
                    AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.select_asset_type));
                } else {
                    if (isSearchOn) {
                        enableSpinner();
                        inv_period = 0;

                        isSearchOn = false;
                        boolean stop = rfidHandler.stopInventory();
                        binding.btnSearch.setText("Start");
                        binding.btnSearch.setBackgroundDrawable(getResources().getDrawable(R.drawable.round_button_green));
                        binding.textPercentage.setBackgroundDrawable(getResources().getDrawable(R.drawable.round_button_green));
                        binding.textPercentage.setText("0 %");
                    } else {

                        inv_period = 0;
                        String assetname = binding.myAutoComplete.getText().toString().trim();

                        disableSpinner();
                        String epc = SharedPreferencesManager.getCompanyCode(context) + SELECTED_ASSET_TYPE_ID;
                        byte[] val = new byte[255];
                        val[0] = (byte) 1;
                        val[1] = (byte) Integer.parseInt("0");
                        if (assetname.equalsIgnoreCase("")) {
                            val[2] = (byte) Integer.parseInt("2");
                        } else {
                            if (db.isValidAssetNameForAssetTypeId(assetname, SELECTED_ASSET_TYPE_ID)) {
                                String serialnumber = db.getAssetSerialNumberByAssetName(assetname);
                                if (serialnumber.equalsIgnoreCase("")) {
                                    val[2] = (byte) Integer.parseInt("2");
                                } else {
                                    val[2] = (byte) Integer.parseInt("6");
                                    epc = epc + serialnumber;

                                }

                                //epc = epc +
                            } else {
                                val[2] = (byte) Integer.parseInt("2");
                            }
                        }

                        val[3] = (byte) 0;//0 - same like filter, 1 = without this filter read all other
                        byte[] data = getHexByteArray(epc);
                        if (val[2] != data.length) {
                            //return false;
                        }
                        System.arraycopy(data, 0, val, 4, val[2]);
                        boolean z = rfidHandler.mDevice.setParamBytes(UHFService.PARAMETER_TAG_FILTER, val);
                        // final boolean b = mDevice.setParamBytes(PARAMETER_TAG_FILTER, epcfilter.getBytes());
                        if (assetname.equalsIgnoreCase("") || db.isValidAssetNameForAssetTypeId(assetname, SELECTED_ASSET_TYPE_ID)) {
                            new Handler().postDelayed(() -> {
                                isSearchOn = true;
                                rfidHandler.startSearchInventory(SharedPreferencesManager.getCompanyCode(context) + SELECTED_ASSET_TYPE_ID);
                                //rfidHandler.startInventory();
                                binding.btnSearch.setText("Stop");
                                binding.textPercentage.setBackgroundDrawable(getResources().getDrawable(R.drawable.round_button_red));
                                binding.btnSearch.setBackgroundDrawable(getResources().getDrawable(R.drawable.round_button_red));
                            }, 100);
                        } else {
                            AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.invalid_asset_for_selected_asset_type));
                        }
                    }
                }
            }
        });
        binding.btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isSearchOn = false;
                rfidHandler.stopInventory();
                binding.btnSearch.setText("Start");
                binding.btnSearch.setBackgroundDrawable(getResources().getDrawable(R.drawable.round_button_green));
                binding.textPercentage.setText("0 %");
                showCustomConfirmationDialog("Are you sure you want to go back", "Cancel");
                if (startStopTimer != null) {
                    startStopTimer.cancel();
                }
                //TODO show confirmation dialog to go back

            }
        });

        binding.spAssetType.setStatusListener(new

                                                      IStatusListener() {
                                                          @Override
                                                          public void spinnerIsOpening() {
                                                              binding.spAssetType.hideEdit();
                                                          }

                                                          @Override
                                                          public void spinnerIsClosing() {
                                                              // LvTags.setVisibility(View.VISIBLE);
                                                          }
                                                      });
        binding.spAssetType.setOnItemSelectedListener(new OnItemSelectedListener() {
                                                                  @Override
                                                                  public void onItemSelected(View view, int position, long id) {

                                                                      if (isSearchOn) {

                                                                          // binding.spAssetType.setSelectedItem(SELECTED_ASSET_POSITION);
                                                                      } else {
                                                                          if (position == 0) {
                                                                              SELECTED_ASSET_TYPE_ITEM = "";
                                                                              SELECTED_ASSET_POSITION = 0;
                                                                              binding.myAutoComplete.setText("");
                                                                          } else {
                                                                              binding.myAutoComplete.setText("");
                                                                              SELECTED_ASSET_POSITION = position;
                                                                              SELECTED_ASSET_TYPE_ITEM = binding.spAssetType.getSelectedItem().toString();
                                                                              SELECTED_ASSET_TYPE_NAME = SELECTED_ASSET_TYPE_ITEM.split(AppConstants.ASSET_TYPE_SPLIT_DATA)[0];
                                                                              SELECTED_ASSET_TYPE_ID = SELECTED_ASSET_TYPE_ITEM.split(AppConstants.ASSET_TYPE_SPLIT_DATA)[1];
                                                                          }
                                                                      }

                                                                  }

                                                                  @Override
                                                                  public void onNothingSelected() {
                                                                      SELECTED_ASSET_TYPE_ITEM = "";
                                                                  }
                                                              });

        AssetUtils.showProgress(context,

                getResources().

                        getString(R.string.uhf_initialization));
        rfidHandler = new

                SeuicGlobalRfidHandler();
        rfidHandler.onCreate(context, new

                RFIDInterface() {
                    @Override
                    public void handleTriggerPress(boolean pressed) {
                        runOnUiThread(() -> {
                            if (pressed) {
                                binding.btnSearch.performClick();
                            }
                        });
                    }

                    @Override
                    public void RFIDInitializationStatus(boolean status) {
                        runOnUiThread(() -> {
                            AssetUtils.hideProgressDialog();

                        });
                    }

                    @Override
                    public void handleLocateTagResponse(int value, int tagSize) {
                        runOnUiThread(() -> {
                            if (isSearchOn) {
                                total_assets_found = tagSize;
                                if (value > maximum_value) {
                                    maximum_value = value;
                                }
                            }
                        });
                    }

                    @Override
                    public void onDataReceived(List<EPC> epcList) {
                        runOnUiThread(() -> {

                        });
                    }
                });

    }

    public void enableSpinner() {
        binding.spAssetType.setEnabled(true);
        binding.spAssetType.setClickable(true);
        binding.myAutoComplete.setEnabled(true);
    }

    public void disableSpinner() {
        binding.spAssetType.setEnabled(false);
        binding.spAssetType.setClickable(false);
        binding.spAssetType.hideEdit();
        binding.myAutoComplete.setEnabled(false);
        // binding.spAssetType.setSelectedItem(SELECTED_ASSET_POSITION);
    }

    private void notifyAdapter() {
        enableSpinner();
        binding.spAssetType.setEnabled(false);
        searchAdapter = new AssetTypeSearchAdapter(context, assetTypeList);
        binding.spAssetType.setAdapter(searchAdapter);
        binding.spAssetType.setEnabled(true);

    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        // On selecting a spinner item
        // type = parent.getItemAtPosition(position).toString();
        binding.myAutoComplete.setText("");

        // Showing selected spinner item
        // Toast.makeText(parent.getContext(), "Selected: " + type, Toast.LENGTH_LONG).show();
    }

    public void onNothingSelected(AdapterView<?> arg0) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onResume() {
        super.onResume();
        enableSpinner();
        rfidHandler.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (beepTimer != null) {
            beepTimer.cancel();
        }
        if (startStopTimer != null) {
            startStopTimer.cancel();
        }
        rfidHandler.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();

        if (beepTimer != null) {
            beepTimer.cancel();
        }
        if (startStopTimer != null) {
            startStopTimer.cancel();
        }
        boolean z = rfidHandler.mDevice.setParamBytes(UHFService.PARAMETER_TAG_FILTER, null);
        rfidHandler.onPause();
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        binding.btnCancel.performClick();

    }

    Dialog customConfirmationDialog;

    public void showCustomConfirmationDialog(String msg, final String action) {
        if (customConfirmationDialog != null) {
            customConfirmationDialog.dismiss();
        }
        customConfirmationDialog = new Dialog(context);
        if (customConfirmationDialog != null) {
            customConfirmationDialog.dismiss();
        }
        customConfirmationDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        customConfirmationDialog.setCancelable(false);
        customConfirmationDialog.setContentView(R.layout.custom_alert_dialog_layout2);
        TextView text = (TextView) customConfirmationDialog.findViewById(R.id.text_dialog);
        text.setText(msg);
        Button dialogButton = (Button) customConfirmationDialog.findViewById(R.id.btn_dialog);
        Button dialogButtonCancel = (Button) customConfirmationDialog.findViewById(R.id.btn_dialog_cancel);
        dialogButton.setText("YES");
        dialogButtonCancel.setText("NO");
        dialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                customConfirmationDialog.dismiss();
                isSearchOn = false;
                rfidHandler.stopInventory();
                if (action.equalsIgnoreCase("CANCEL")) {
                    finish();
                }
            }
        });
        dialogButtonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                customConfirmationDialog.dismiss();
            }
        });
        // customConfirmationDialog.getWindow().getAttributes().windowAnimations = R.style.SlideBottomUpAnimation;
        customConfirmationDialog.show();
    }
}