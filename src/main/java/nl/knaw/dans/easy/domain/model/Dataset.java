package nl.knaw.dans.easy.domain.model;

import java.io.File;
import java.util.List;
import java.util.Set;

import nl.knaw.dans.common.lang.dataset.AccessCategory;
import nl.knaw.dans.common.lang.dataset.DatasetState;
import nl.knaw.dans.common.lang.repo.DmoNamespace;
import nl.knaw.dans.easy.domain.exceptions.DomainException;
import nl.knaw.dans.easy.domain.exceptions.ObjectNotFoundException;
import nl.knaw.dans.easy.domain.model.disciplinecollection.DisciplineContainer;
import nl.knaw.dans.easy.domain.model.emd.EasyMetadata;
import nl.knaw.dans.easy.domain.model.emd.types.ApplicationSpecific.MetadataFormat;
import nl.knaw.dans.easy.domain.model.emd.types.IsoDate;
import nl.knaw.dans.easy.domain.model.user.EasyUser;
import nl.knaw.dans.easy.domain.model.user.Group;

import org.joda.time.DateTime;

public interface Dataset extends DatasetItemContainer
{
    AccessCategory DEFAULT_ACCESS_CATEGORY = AccessCategory.OPEN_ACCESS;
    
    String NAME_SPACE_VALUE = "easy-dataset";

    DmoNamespace NAMESPACE = new DmoNamespace(NAME_SPACE_VALUE);

    EasyMetadata getEasyMetadata();

    AdministrativeMetadata getAdministrativeMetadata();

    DatasetState getAdministrativeState();

    PermissionSequenceList getPermissionSequenceList();

    String getPreferredTitle();

    boolean hasDepositor(EasyUser user);

    boolean hasDepositor(String userId);
    

    EasyUser getDepositor();

    int getAccessProfileFor(EasyUser user);

    /**
     * Get the AccessCategory the depositor set on the dataset when depositing.
     * 
     * @return AccessCategory the depositor set on the dataset when depositing
     */
    AccessCategory getAccessCategory();

    DateTime getDateAvailable();

    boolean isUnderEmbargo(DateTime atDate);

    boolean isUnderEmbargo();

    List<AccessCategory> getChildVisibility();

    List<AccessCategory> getChildAccessibility();

    boolean hasPermissionRestrictedItems();

    boolean hasGroupRestrictedItems();

    Set<Group> getGroups();

    boolean addGroup(Group group);

    boolean removeGroup(Group group);

    MetadataFormat getMetadataFormat();

    List<DisciplineContainer> getParentDisciplines() throws DomainException, ObjectNotFoundException;

    List<DisciplineContainer> getLeafDisciplines() throws ObjectNotFoundException, DomainException;

    boolean isPermissionGrantedTo(EasyUser user);

    boolean isGroupAccessGrantedTo(EasyUser user);

    boolean hasVisibleItems(EasyUser user);

    String getPersistentIdentifier();

    void setLicenseContent(byte[] content);

    IsoDate getDateSubmitted();

    void setAdditionalLicenseContent(File file);
}
