import * as React from 'react';
import { Button, Card, Spin, Row, Col } from 'antd';
import { PrepareHelp, RunHelp, CreateHelp, Label } from './';
import { Provisioning } from '../../../components';
import { Workspace } from '../../../models/Workspace';
import { Cluster } from '../../../models/Cluster';
import { ProvisioningType, FeatureFlagType } from '../../../constants';
import { Feature } from '../../../components';

interface Props {
  workspace: Workspace;
  services: any;
  loading: boolean;
  cluster?: Cluster;
  provisioning: ProvisioningType;
  workspaceFetched: boolean;
  requestWorkspace: () => void;
}

const PersonalWorkspace = ({
  workspace,
  services,
  requestWorkspace,
  loading,
  provisioning,
  workspaceFetched,
}: Props) => {
  if (loading) {
    return (
      <div
        style={{
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          padding: 24,
        }}
      >
        <Spin />
      </div>
    );
  }

  return (
    <div style={{ flex: 1 }}>
      {workspace && workspace.data ? (
        <Card bodyStyle={{ display: 'flex', flexDirection: 'column' }}>
          <Label>Your Personal Workspace</Label>
          <Provisioning provisioning={provisioning} />
          <div>
            <div
              style={{
                display: 'flex',
                justifyContent: 'center',
                textAlign: 'center',
                fontWeight: 'bold',
                fontSize: '12px',
              }}
            >
              <div style={{ padding: '10px' }}>
                <div>HDFS LOCATION</div>
                <div style={{ fontWeight: 200 }}>{workspace.data[0].location}</div>
              </div>
              <div style={{ padding: '10px' }}>
                <div>HIVE NAMESPACE</div>
                <div style={{ fontWeight: 200 }}>{workspace.data[0].name}</div>
              </div>
              <Feature flag={FeatureFlagType.Application}>
                <div style={{ padding: '10px' }}>
                  <div>RESOURCE POOL</div>
                  <div style={{ fontWeight: 200 }}>{workspace.processing[0].pool_name}</div>
                </div>
              </Feature>
              {!!workspace.topics[0] && !!workspace.topics[0].name && (
                <Feature flag={FeatureFlagType.Messaging}>
                  <div style={{ padding: '10px' }}>
                    <div>KAFKA TOPIC</div>
                    <div style={{ fontWeight: 200 }}>{workspace.topics[0].name}</div>
                  </div>
                </Feature>
              )}
            </div>
            <Row gutter={12}>
              <Col span={24} xl={8} style={{ marginTop: 10 }}>
                <PrepareHelp location={workspace.data[0].location} namespace={workspace.data[0].name} />
              </Col>
              <Col span={24} xl={8} style={{ marginTop: 10 }}>
                <CreateHelp
                  host={services.hive.thrift && services.hive.thrift[0].host}
                  port={services.hive.thrift && services.hive.thrift[0].port}
                  namespace={workspace.data[0].name}
                />
              </Col>
              <Col span={24} xl={8} style={{ marginTop: 10 }}>
                <RunHelp queue={workspace.processing[0].pool_name} />
              </Col>
            </Row>
          </div>
        </Card>
      ) : (
        <Card bodyStyle={{ textAlign: 'center' }}>
          {workspaceFetched ? (
            <React.Fragment>
              <h3>You don't have a personal workspace yet!</h3>
              <Button type="primary" onClick={requestWorkspace}>
                Create One Now
              </Button>
            </React.Fragment>
          ) : (
            <h3 style={{ color: 'red' }}>Error retrieving personal workspace.</h3>
          )}
        </Card>
      )}
    </div>
  );
};

export default PersonalWorkspace;
