package com.atask;

final class Json {

    protected static JsonObject createObject() {
        return new JsonObject();
    }

    protected static JsonArray createArray() {
        return new JsonArray();
    }

    static class JsonObject {
        private final StringBuilder builder = new StringBuilder();

        private JsonObject() {
            builder.append("{");
        }

        public JsonObject put(String name, Object value) {
            builder.append("\"").append(name).append("\":");
            if (value == null) {
                builder.append("null");
            } else if (value instanceof CharSequence) {
                builder.append("\"").append(value.toString()).append("\"");
            } else {
                builder.append(value.toString());
            }
            builder.append(",");
            return this;
        }

        public JsonObject end() {
            if(builder.charAt(builder.length() - 1) == ',') {
                builder.deleteCharAt(builder.length() - 1);
            }
            builder.append("}");
            return this;
        }

        @Override
        public String toString() {
            return builder.toString();
        }
    }

    static class JsonArray {
        private final StringBuilder builder = new StringBuilder();

        private JsonArray() {
            builder.append("[");
        }

        public JsonArray add(Object value) {
            if (value == null) {
                builder.append("null");
            } else if (value instanceof CharSequence) {
                builder.append("\"").append(value).append("\"");
            } else {
                builder.append(value.toString());
            }
            builder.append(",");
            return this;
        }

        public JsonArray end() {
            if(builder.charAt(builder.length() - 1) == ',') {
                builder.deleteCharAt(builder.length() - 1);
            }
            builder.append("]");
            return this;
        }

        @Override
        public String toString() {
            return builder.toString();
        }
    }

}
