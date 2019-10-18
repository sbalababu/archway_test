import { Avatar, Card } from 'antd';
import * as React from 'react';
import { ServiceLinks, Status, Statusable } from '../../../models/Cluster';
import { Colors } from '../../../components';

export interface Props {
  name: string;
  status: Status<Statusable>;
  links: ServiceLinks<Statusable>;
  index: number;
}

const ServiceDisplay = ({ name, status, links, index }: Props) => (
  <Card style={{ flex: 1, marginLeft: index === 0 ? 0 : 25 }} actions={links.links}>
    <Card.Meta
      title={name}
      avatar={
        <Avatar size="small" style={{ boxShadow: status.glowColorText(), backgroundColor: Colors.Green.toString() }} />
      }
      style={{ display: 'flex', justifyContent: 'center' }}
    />
  </Card>
);

export default ServiceDisplay;
