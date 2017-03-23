package de.mein.android.view;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import de.mein.auth.data.db.Certificate;
import mein.de.meindrive.R;

/**
 * Created by xor on 3/16/17.
 */

public class KnownCertListAdapter extends MeinListAdapter<Certificate> {

    public KnownCertListAdapter(Context context) {
        super(context);
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Certificate certificate = items.get(position);
        View v = layoutInflator.inflate(R.layout.line_3_list_item, null);
        TextView lblName = (TextView) v.findViewById(R.id.lbl1);
        lblName.setText(certificate.getName().v());
        TextView lblAddress = (TextView) v.findViewById(R.id.lbl2);
        String line2 = (certificate.getAddress().v() != null) ? certificate.getAddress().v() + " " : "";
        lblAddress.setText(line2);
        TextView lblPorts = (TextView) v.findViewById(R.id.lbl3);
        String line3 = (certificate.getPort().v() != null) ? "Port: " + certificate.getPort().v() + " " : "";
        line3 += (certificate.getCertDeliveryPort().v() != null) ? "CertPort: " + certificate.getCertDeliveryPort().v() : "";
        lblPorts.setText(line3);
        return v;
    }

}