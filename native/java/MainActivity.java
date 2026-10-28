package com.clipcollage.app;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(CollageNativePlugin.class);
        super.onCreate(savedInstanceState);
    }
}
