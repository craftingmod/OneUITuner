package info.build;

import android.app.Activity;
import android.os.Bundle;
import tk.zwander.oneuituner.R;

public class DummyActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.build_info);
    }
}
