package hyperion.jira.extension.ao.models;

import net.java.ao.Entity;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;

@Table("HyperionHttpEnd")
public interface AOHTTPEndpoint extends Entity {
    @StringLength(StringLength.UNLIMITED)
    String getDescription();
    @StringLength(StringLength.UNLIMITED)
    void setDescription(String description);

    @StringLength(50)
    String getEndpointName();
    @StringLength(50)
    void setEndpointName(String name);

    String getResourceID();
    void setResourceID(String resourceID);

    boolean isAllowAnonymous();
    void setAllowAnonymous(boolean allowAnonymous);

    boolean isMethodGet();
    void setMethodGet(boolean get);

    boolean isMethodHead();
    void setMethodHead(boolean head);

    boolean isMethodPost();
    void setMethodPost(boolean post);

    boolean isMethodPut();
    void setMethodPut(boolean put);

    boolean isMethodDelete();
    void setMethodDelete(boolean delete);

    boolean isMethodOptions();
    void setMethodOptions(boolean options);

    boolean isMethodTrace();
    void setMethodTrace(boolean trace);

    boolean isMethodPatch();
    void setMethodPatch(boolean patch);

    @StringLength(StringLength.UNLIMITED)
    String getJSONConfiguration();
    @StringLength(StringLength.UNLIMITED)
    void setJSONConfiguration(String jsonConfiguration);
}
