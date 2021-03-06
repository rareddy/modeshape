/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.schematic.internal.document;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.modeshape.schematic.document.Array;
import org.modeshape.schematic.document.Binary;
import org.modeshape.schematic.document.Code;
import org.modeshape.schematic.document.CodeWithScope;
import org.modeshape.schematic.document.Document;
import org.modeshape.schematic.document.EditableArray;
import org.modeshape.schematic.document.EditableDocument;
import org.modeshape.schematic.document.MaxKey;
import org.modeshape.schematic.document.MinKey;
import org.modeshape.schematic.document.Null;
import org.modeshape.schematic.document.ObjectId;
import org.modeshape.schematic.document.Symbol;

public class DocumentEditor implements EditableDocument {

    /** The serialVersionUID */
    private static final long serialVersionUID = 1L;

    private final MutableDocument document;
    protected final DocumentValueFactory factory;

    /**
     * Return the document that was edited.
     * 
     * @param document the document to be edited
     */
    public DocumentEditor( MutableDocument document ) {
        assert document != null;
        this.document = document;
        this.factory = DefaultDocumentValueFactory.INSTANCE;
    }

    /**
     * Return the document that was edited.
     * 
     * @param document the document to be edited
     * @param factory the factory that should be used to create value objects
     */
    public DocumentEditor( MutableDocument document,
                           DocumentValueFactory factory ) {
        assert document != null;
        this.document = document;
        this.factory = factory != null ? factory : DefaultDocumentValueFactory.INSTANCE;
    }

    @Override
    public DocumentEditor clone() {
        return new DocumentEditor(this.document.clone(), factory);
    }

    @Override
    public DocumentEditor with( Map<String, Object> changedFields ) {
        return new DocumentEditor((MutableDocument)this.document.with(changedFields), factory);
    }

    @Override
    public Document with( String fieldName,
                          Object value ) {
        return new DocumentEditor((MutableDocument)this.document.with(fieldName, value), factory);
    }

    @Override
    public DocumentEditor with( ValueTransformer transformer ) {
        return new DocumentEditor((MutableDocument)this.document.with(transformer), factory);
    }

    @Override
    public Document withVariablesReplaced( Properties properties ) {
        return new DocumentEditor((MutableDocument)this.document.withVariablesReplaced(properties), factory);
    }

    @Override
    public Document withVariablesReplacedWithSystemProperties() {
        return new DocumentEditor((MutableDocument)this.document.withVariablesReplacedWithSystemProperties(), factory);
    }

    @Override
    public Document unwrap() {
        return document;
    }

    @Override
    public MutableDocument asMutableDocument() {
        return document;
    }

    @Override
    public Object get( String name ) {
        return document.get(name);
    }

    @Override
    public Boolean getBoolean( String name ) {
        return document.getBoolean(name);
    }

    @Override
    public boolean getBoolean( String name,
                               boolean defaultValue ) {
        return document.getBoolean(name, defaultValue);
    }

    public Object put( String name,
                       Object value ) {
        return doSetValue(name, value);
    }

    @Override
    public void putAll( Document object ) {
        doSetAllValues(object);
    }

    @Override
    public void putAll( Map<? extends String, ?> map ) {
        doSetAllValues(map);
    }

    @Override
    public void merge( Document other ) {
        if (other == this) return;
        for (Field field : other.fields()) {
            Document otherDoc = field.getValueAsDocument();
            if (!Null.matches(otherDoc)) {
                // Get the corresponding value in this document ...
                EditableDocument thisField = getDocument(field.getName());
                if (!Null.matches(thisField)) {
                    // There are docs in both sides, so merge them ...
                    thisField.merge(otherDoc);
                } else {
                    // There is not a document on this side (perhaps another value), so replace with that other doc ...
                    doSetValue(field.getName(), otherDoc);
                }
            } else {
                // The field is something other than a document, so just set it on this document ...
                doSetValue(field.getName(), field.getValue());
            }
        }
    }

    @Override
    public Object remove( String name ) {
        return document.remove(name);
    }

    @Override
    public Integer getInteger( String name ) {
        return document.getInteger(name);
    }

    @Override
    public int getInteger( String name,
                           int defaultValue ) {
        return document.getInteger(name, defaultValue);
    }

    @Override
    public Long getLong( String name ) {
        return document.getLong(name);
    }

    @Override
    public long getLong( String name,
                         long defaultValue ) {
        return document.getLong(name, defaultValue);
    }

    @Override
    public Double getDouble( String name ) {
        return document.getDouble(name);
    }

    @Override
    public double getDouble( String name,
                             double defaultValue ) {
        return document.getDouble(name, defaultValue);
    }

    @Override
    public Number getNumber( String name ) {
        return document.getNumber(name);
    }

    @Override
    public Number getNumber( String name,
                             Number defaultValue ) {
        return document.getNumber(name, defaultValue);
    }
    
    @Override
    public Date getDate(String name) {
        return document.getDate(name);
    }
    
    @Override
    public String getString( String name ) {
        return document.getString(name);
    }

    @Override
    public String getString( String name,
                             String defaultValue ) {
        return document.getString(name, defaultValue);
    }

    @Override
    public EditableArray getArray( String name ) {
        return editable(document.getArray(name), name);
    }

    @Override
    public EditableArray getOrCreateArray( String name ) {
        List<?> existing = document.getArray(name);
        return existing != null ? editable(existing, name) : setArray(name);
    }

    @Override
    public EditableDocument getDocument( String name ) {
        return editable(document.getDocument(name), name);
    }

    @Override
    public EditableDocument getOrCreateDocument( String name ) {
        Document existing = document.getDocument(name);
        return existing != null ? editable(existing, name) : setDocument(name);
    }

    @Override
    public boolean isNull( String name ) {
        return document.isNull(name);
    }

    @Override
    public boolean isNullOrMissing( String name ) {
        return document.isNullOrMissing(name);
    }

    @Override
    public MaxKey getMaxKey( String name ) {
        return document.getMaxKey(name);
    }

    @Override
    public MinKey getMinKey( String name ) {
        return document.getMinKey(name);
    }

    @Override
    public Code getCode( String name ) {
        return document.getCode(name);
    }

    @Override
    public CodeWithScope getCodeWithScope( String name ) {
        return document.getCodeWithScope(name);
    }

    @Override
    public ObjectId getObjectId( String name ) {
        return document.getObjectId(name);
    }

    @Override
    public Binary getBinary( String name ) {
        return document.getBinary(name);
    }

    @Override
    public Symbol getSymbol( String name ) {
        return document.getSymbol(name);
    }

    @Override
    public Pattern getPattern( String name ) {
        return document.getPattern(name);
    }

    @Override
    public UUID getUuid( String name ) {
        return document.getUuid(name);
    }

    @Override
    public UUID getUuid( String name,
                         UUID defaultValue ) {
        return document.getUuid(name, defaultValue);
    }

    @Override
    public int getType( String name ) {
        return document.getType(name);
    }

    @Override
    public Map<String, ?> toMap() {
        return document.toMap();
    }

    @Override
    public Iterable<Field> fields() {
        return document.fields();
    }

    @Override
    public boolean containsField( String name ) {
        return document.containsField(name);
    }

    @Override
    public boolean containsAll( Document document ) {
        return this.document.containsAll(document);
    }

    @Override
    public Set<String> keySet() {
        return document.keySet();
    }

    @Override
    public int size() {
        return document.size();
    }

    @Override
    public boolean isEmpty() {
        return document.isEmpty();
    }

    @Override
    public void removeAll() {
        document.removeAll();
    }

    @Override
    public EditableDocument set( String name,
                                 Object value ) {
        doSetValue(name, value);
        return this;
    }

    @Override
    public EditableDocument setBoolean( String name,
                                        boolean value ) {
        doSetValue(name, factory.createBoolean(value));
        return this;
    }

    @Override
    public EditableDocument setNumber( String name,
                                       int value ) {
        doSetValue(name, factory.createInt(value));
        return this;
    }

    @Override
    public EditableDocument setNumber( String name,
                                       long value ) {
        doSetValue(name, factory.createLong(value));
        return this;
    }

    @Override
    public EditableDocument setNumber( String name,
                                       float value ) {
        doSetValue(name, factory.createDouble(value));
        return this;
    }

    @Override
    public EditableDocument setNumber( String name,
                                       double value ) {
        doSetValue(name, factory.createDouble(value));
        return this;
    }

    @Override
    public EditableDocument setString( String name,
                                       String value ) {
        doSetValue(name, factory.createString(value));
        return this;
    }

    @Override
    public EditableDocument setSymbol( String name,
                                       String value ) {
        doSetValue(name, factory.createSymbol(value));
        return this;
    }

    @Override
    public EditableDocument setDocument( String name ) {
        BasicDocument doc = new BasicDocument();
        doSetValueIfAbsent(name, doc);
        return editable(doc, name);
    }

    @Override
    public EditableDocument setDocument( String name,
                                         Document document ) {
        if (document instanceof DocumentEditor) document = ((DocumentEditor)document).asMutableDocument();
        doSetValue(name, document);
        return editable(document, name);
    }

    @Override
    public EditableArray setArray( String name ) {
        List<?> array = new BasicArray();
        doSetValueIfAbsent(name, array);
        return editable(array, name);
    }

    @Override
    public EditableArray setArray( String name,
                                   Array array ) {
        if (array instanceof ArrayEditor) array = ((ArrayEditor)array).unwrap();
        doSetValue(name, array);
        return editable((List<?>)array, name);
    }

    @Override
    public EditableArray setArray( String name,
                                   Object... values ) {
        List<?> array = new BasicArray(values);
        doSetValue(name, array);
        return editable(array, name);
    }

    @Override
    public EditableDocument setDate( String name,
                                     Date value ) {
        doSetValue(name, value);
        return this;
    }

    @Override
    public EditableDocument setDate( String name,
                                     String isoDate ) throws ParseException {
        doSetValue(name, factory.createDate(isoDate));
        return this;
    }

    @Override
    public EditableDocument setTimestamp( String name,
                                          int timeInSeconds,
                                          int increment ) {
        doSetValue(name, factory.createTimestamp(timeInSeconds, increment));
        return this;
    }

    @Override
    public EditableDocument setObjectId( String name,
                                         String hex ) {
        doSetValue(name, factory.createObjectId(hex));
        return this;
    }

    @Override
    public EditableDocument setObjectId( String name,
                                         byte[] bytes ) {
        doSetValue(name, factory.createObjectId(bytes));
        return this;
    }

    @Override
    public EditableDocument setObjectId( String name,
                                         int time,
                                         int machine,
                                         int process,
                                         int inc ) {
        doSetValue(name, factory.createObjectId(time, machine, process, inc));
        return this;
    }

    @Override
    public EditableDocument setRegularExpression( String name,
                                                  String pattern ) {
        doSetValue(name, factory.createRegex(pattern, null));
        return this;
    }

    @Override
    public EditableDocument setRegularExpression( String name,
                                                  String pattern,
                                                  int flags ) {
        doSetValue(name, factory.createRegex(pattern, BsonUtils.regexFlagsFor(flags)));
        return this;
    }

    @Override
    public EditableDocument setNull( String name ) {
        doSetValue(name, factory.createNull());
        return this;
    }

    @Override
    public EditableDocument setBinary( String name,
                                       byte type,
                                       byte[] data ) {
        doSetValue(name, factory.createBinary(type, data));
        return this;
    }

    @Override
    public EditableDocument setUuid( String name,
                                     UUID uuid ) {
        doSetValue(name, uuid);
        return this;
    }

    @Override
    public EditableDocument setCode( String name,
                                     String code,
                                     boolean includeScope ) {
        if (includeScope) {
            BasicDocument scope = new BasicDocument();
            doSetValue(name, factory.createCode(code, scope));
            return editable(scope, name);
        }
        doSetValue(name, factory.createCode(code));
        return this;
    }

    @Override
    public EditableDocument setCode( String name,
                                     String code,
                                     Document scope ) {
        if (scope != null) {
            doSetValue(name, factory.createCode(code, scope));
            return editable(scope, name);
        }
        doSetValue(name, factory.createCode(code));
        return this;
    }

    /**
     * The method that does the actual setting for all of the <code>set...</code> methods. This method may be overridden by
     * subclasses when additional work needs to be performed during the set operations.
     * 
     * @param name the name of the field being set
     * @param value the new value
     * @return the old value, or null if there was no existing value
     */
    protected Object doSetValue( String name,
                                 Object value ) {
        if (value == null) {
            value = Null.getInstance();
        } else {
            value = Utility.unwrap(value);
        }
        return document.put(name, value);
    }

    /**
     * The method that does the actual setting for all of the <code>set...</code> methods. This method may be overridden by
     * subclasses when additional work needs to be performed during the set operations.
     * 
     * @param name the name of the field being set
     * @param value the new value
     * @return the old value, or null if there was no existing value
     */
    protected Object doSetValueIfAbsent( String name,
                                         Object value ) {
        if (value == null) {
            value = Null.getInstance();
        } else {
            value = Utility.unwrap(value);
        }
        return document.put(name, value);
    }

    /**
     * The method that does the actual setting for all of the {@link #putAll(Document)} method. This method may be overridden by
     * subclasses when additional work needs to be performed during this operation.
     * 
     * @param values the document containing the fields to be added
     */
    protected void doSetAllValues( Document values ) {
        if (values != null) {
            values = Utility.unwrap(values);
            document.putAll(values);
        }
    }

    /**
     * The method that does the actual setting for all of the {@link #putAll(Map)} method. This method may be overridden by
     * subclasses when additional work needs to be performed during this operation.
     * 
     * @param values the map containing the fields to be added
     */
    protected void doSetAllValues( Map<? extends String, ?> values ) {
        if (values != null) {
            document.putAll(Utility.unwrapValues(values));
        }
    }

    protected EditableDocument editable( Document doc,
                                         String fieldName ) {
        if (doc == null) return null;
        assert !(doc instanceof DocumentEditor) : "The document value should not be a DocumentEditor instance";
        if (doc instanceof MutableArray) {
            return createEditableArray((MutableArray)doc, fieldName, factory);
        }
        assert doc instanceof MutableDocument;
        return createEditableDocument((MutableDocument)doc, fieldName, factory);
    }

    protected EditableArray editable( List<?> array,
                                      String fieldName ) {
        if (array == null) return null;
        assert !(array instanceof ArrayEditor) : "The array value should not be an ArrayEditor instance";
        return createEditableArray((BasicArray)array, fieldName, factory);
    }

    protected EditableDocument createEditableDocument( MutableDocument document,
                                                       String fieldName,
                                                       DocumentValueFactory factory ) {
        return new DocumentEditor(document, factory);
    }

    protected EditableArray createEditableArray( MutableArray array,
                                                 String fieldName,
                                                 DocumentValueFactory factory ) {
        return new ArrayEditor(array, factory);
    }

    @Override
    public String toString() {
        return document.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MutableDocument) && ! (o instanceof DocumentEditor)) {
            return false;
        }
        
        if (o instanceof MutableDocument) {
            return Objects.equals(document, o); 
        } else {
            DocumentEditor that = (DocumentEditor) o;
            return Objects.equals(this.document, that.document);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(document);
    }
}
