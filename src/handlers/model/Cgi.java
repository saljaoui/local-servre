package handlers.model;

import java.util.Map;

public class Cgi {

        private boolean enabled;
        private String binDir;
        private Map<String, String> byExtension;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getBinDir() {
            return binDir;
        }

        public void setBinDir(String binDir) {
            this.binDir = binDir;
        }

        public Map<String, String> getByExtension() {
            return byExtension;
        }

        public void setByExtension(Map<String, String> byExtension) {
            this.byExtension = byExtension;
        }

        public String getInterpreterForExtension(String ext) {
            if (byExtension == null) {
                return null;
            }
            return byExtension.get(ext.toLowerCase());
        }

        @Override
        public String toString() {
            return "Cgi{enabled=" + enabled + ", binDir='" + binDir
                    + "', extensions=" + (byExtension != null ? byExtension.keySet() : "[]") + "}";
        }
    }
