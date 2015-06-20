package org.isoron.base;

import android.os.Bundle;

public interface AmbientModeListener
{
    public void onEnterAmbient(Bundle ambientDetails);

    public void onExitAmbient();

    public void onUpdateAmbient();
}
