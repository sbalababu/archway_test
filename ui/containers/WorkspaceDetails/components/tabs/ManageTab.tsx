import * as React from 'react';
import { Row, Col } from 'antd';

import { ManageCard, ChangeOwnerCard } from '../cards';
import { ProvisioningType, ModalType } from '../../../../constants';

interface Props {
  provisioning: ProvisioningType;
  showModal: (e: React.MouseEvent, type: ModalType) => void;
}

const ManageTab = ({ provisioning, showModal }: Props) => (
  <div style={{ padding: 16, fontSize: 17 }}>
    <Row gutter={16} type="flex" style={{ marginBottom: 16 }}>
      <Col style={{ flex: 1 }}>
        <ManageCard
          title="PROVISION"
          showModal={showModal}
          modalType={ModalType.ProvisionWorkspace}
          buttonText="Provision"
          disabled={false}
        >
          Provisioning a workspace manually is not necessary under normal circumstances. Automatic provisioning
          scheduled and will automatically re-attempt errored workspaces. This manual provisioning can be used to
          reprovision a workspace that has already successfully provisioned.
        </ManageCard>
      </Col>
      <Col style={{ flex: 1 }}>
        <ManageCard
          title="HIDE"
          showModal={showModal}
          modalType={ModalType.DeleteWorkspace}
          buttonText="Hide Workspace"
          disabled={false}
        >
          After clicking this button users and approvers will not be able to see this workspace in any workspace
          listings.
        </ManageCard>
      </Col>
      <Col style={{ flex: 1 }}>
        <ManageCard
          title="DEPROVISION"
          showModal={showModal}
          modalType={ModalType.DeprovisionWorkspace}
          buttonText="Deprovision"
          disabled={provisioning === ProvisioningType.Pending}
        >
          Deprovisioning a workspace will attempt to remove components of the workspace including Hive databases, AD
          groups, and Sentry roles. Deleting a workspace will not remove any data on HDFS.
        </ManageCard>
      </Col>
    </Row>
    <Row gutter={16} type="flex" justify="center">
      <Col sm={{ span: 24 }} lg={{ span: 8 }} style={{ textAlign: 'center' }}>
        <ChangeOwnerCard
          title="CHANGE WORKSPACE OWNER"
          showModal={showModal}
          modalType={ModalType.ChangeOwner}
          buttonText="Change Owner"
        />
      </Col>
    </Row>
  </div>
);

export default ManageTab;
