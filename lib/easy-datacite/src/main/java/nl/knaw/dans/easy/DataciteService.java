package nl.knaw.dans.easy;

import javax.ws.rs.core.Response;

import nl.knaw.dans.pf.language.emd.EasyMetadata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;

public class DataciteService {

    private static final String CONTENT_TYPE = "application/xml;charset=UTF-8";

    private static final Logger logger = LoggerFactory.getLogger(DataciteService.class);

    private final DataciteServiceConfiguration configuration;

    private DataciteResourcesBuilder resourcesBuilder;

    public DataciteService(DataciteServiceConfiguration configuration) {
        this.configuration = configuration;
        resourcesBuilder = new DataciteResourcesBuilder(configuration.getXslEmd2datacite());
    }

    public void create(EasyMetadata... emds) throws DataciteServiceException {
        send(0, "Creating %s DOIs resulted in %s. First DOI %s", emds);
    }

    public void update(EasyMetadata... emds) throws DataciteServiceException {
        send(1, "Updating %s DOIs resulted in %s. First DOI %s", emds);
    }

    private void send(int countPositionInResponse, String format, EasyMetadata... emds) throws DataciteServiceException {
        String result = post(resourcesBuilder.create(emds));
        String firstDOI = emds[0].getEmdIdentifier().getDansManagedDoi();
        String message = String.format(format, emds.length, result, firstDOI);
        if (getCount(countPositionInResponse, result) == emds.length)
            logger.info(message);
        else {
            if (getCount(0, result) + getCount(1, result) == emds.length)
                logger.warn(message);
            else {
                logger.error(message);
                throw new DataciteServiceException(message);
            }
        }
    }

    /** @return something like "dois created: 0, dois updated: 0 (TEST OK)" */
    private String post(String content) throws DataciteServiceException {
        try {
            ClientResponse response = createWebResource().type(CONTENT_TYPE).post(ClientResponse.class, content);
            if (response.getStatus() != Response.Status.OK.getStatusCode()) {
                throw new DataciteServiceException("DOI post failed : HTTP error code : " + response.getStatus());
            }
            return response.getEntity(String.class);
        }
        catch (UniformInterfaceException e) {
            throw createPostFailedException(e);
        }
        catch (ClientHandlerException e) {
            throw createPostFailedException(e);
        }
    }

    private WebResource createWebResource() {
        Client client = Client.create();
        client.addFilter(new HTTPBasicAuthFilter(configuration.getUsername(), configuration.getPassword()));
        return client.resource(configuration.getDoiRegistrationUri());
    }

    private DataciteServiceException createPostFailedException(Exception e) {
        return new DataciteServiceException("DOI post request failed: " + e.getMessage(), e);
    }

    /** @return the i-th number from something like "dois created: 0, dois updated: 0 (TEST OK)" */
    private int getCount(int i, String postResult) {
        return Integer.parseInt(postResult.split(",")[i].replaceAll("\\D+", ""));
    }
}