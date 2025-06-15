package main

import (
	"github.com/pulumi/pulumi-aws/sdk/v6/go/aws"
	"github.com/pulumi/pulumi-aws/sdk/v6/go/aws/dynamodb"
	"github.com/pulumi/pulumi-aws/sdk/v6/go/aws/ec2"
	"github.com/pulumi/pulumi/sdk/v3/go/pulumi"
)

func main() {
	pulumi.Run(func(ctx *pulumi.Context) error {
		// Get the AWS region
		region, err := aws.GetRegion(ctx, &aws.GetRegionArgs{})
		if err != nil {
			return err
		}

		// Create a VPC
		vpc, err := ec2.NewVpc(ctx, "flink-ddb-vpc", &ec2.VpcArgs{
			CidrBlock:          pulumi.String("10.0.0.0/16"),
			EnableDnsHostnames: pulumi.Bool(true),
			EnableDnsSupport:   pulumi.Bool(true),
			Tags: pulumi.StringMap{
				"Name": pulumi.String("flink-ddb-vpc"),
			},
		})
		if err != nil {
			return err
		}

		// Create a public subnet
		subnet, err := ec2.NewSubnet(ctx, "flink-ddb-subnet", &ec2.SubnetArgs{
			VpcId:               vpc.ID(),
			CidrBlock:           pulumi.String("10.0.1.0/24"),
			AvailabilityZone:    pulumi.String(region.Name + "a"),
			MapPublicIpOnLaunch: pulumi.Bool(true),
			Tags: pulumi.StringMap{
				"Name": pulumi.String("flink-ddb-subnet"),
			},
		})
		if err != nil {
			return err
		}

		// Create an Internet Gateway
		igw, err := ec2.NewInternetGateway(ctx, "flink-ddb-igw", &ec2.InternetGatewayArgs{
			VpcId: vpc.ID(),
			Tags: pulumi.StringMap{
				"Name": pulumi.String("flink-ddb-igw"),
			},
		})
		if err != nil {
			return err
		}

		// Create a route table
		routeTable, err := ec2.NewRouteTable(ctx, "flink-ddb-rt", &ec2.RouteTableArgs{
			VpcId: vpc.ID(),
			Routes: ec2.RouteTableRouteArray{
				&ec2.RouteTableRouteArgs{
					CidrBlock: pulumi.String("0.0.0.0/0"),
					GatewayId: igw.ID(),
				},
			},
			Tags: pulumi.StringMap{
				"Name": pulumi.String("flink-ddb-rt"),
			},
		})
		if err != nil {
			return err
		}

		// Associate the route table with the subnet
		_, err = ec2.NewRouteTableAssociation(ctx, "flink-ddb-rta", &ec2.RouteTableAssociationArgs{
			SubnetId:     subnet.ID(),
			RouteTableId: routeTable.ID(),
		})
		if err != nil {
			return err
		}

		// Create a security group
		sg, err := ec2.NewSecurityGroup(ctx, "flink-ddb-sg", &ec2.SecurityGroupArgs{
			VpcId:       vpc.ID(),
			Description: pulumi.String("Security group for Flink DynamoDB lab"),
			Ingress: ec2.SecurityGroupIngressArray{
				// SSH
				&ec2.SecurityGroupIngressArgs{
					Protocol:   pulumi.String("tcp"),
					FromPort:   pulumi.Int(22),
					ToPort:     pulumi.Int(22),
					CidrBlocks: pulumi.StringArray{pulumi.String("0.0.0.0/0")},
				},
				// Flink UI
				&ec2.SecurityGroupIngressArgs{
					Protocol:   pulumi.String("tcp"),
					FromPort:   pulumi.Int(8081),
					ToPort:     pulumi.Int(8081),
					CidrBlocks: pulumi.StringArray{pulumi.String("0.0.0.0/0")},
				},
				// HTTP
				&ec2.SecurityGroupIngressArgs{
					Protocol:   pulumi.String("tcp"),
					FromPort:   pulumi.Int(80),
					ToPort:     pulumi.Int(80),
					CidrBlocks: pulumi.StringArray{pulumi.String("0.0.0.0/0")},
				},
				// HTTPS
				&ec2.SecurityGroupIngressArgs{
					Protocol:   pulumi.String("tcp"),
					FromPort:   pulumi.Int(443),
					ToPort:     pulumi.Int(443),
					CidrBlocks: pulumi.StringArray{pulumi.String("0.0.0.0/0")},
				},
			},
			Egress: ec2.SecurityGroupEgressArray{
				&ec2.SecurityGroupEgressArgs{
					Protocol:   pulumi.String("-1"),
					FromPort:   pulumi.Int(0),
					ToPort:     pulumi.Int(0),
					CidrBlocks: pulumi.StringArray{pulumi.String("0.0.0.0/0")},
				},
			},
			Tags: pulumi.StringMap{
				"Name": pulumi.String("flink-ddb-sg"),
			},
		})
		if err != nil {
			return err
		}

		// Get the latest Amazon Linux 2023 AMI
		ami, err := ec2.LookupAmi(ctx, &ec2.LookupAmiArgs{
			MostRecent: pulumi.BoolRef(true),
			Filters: []ec2.GetAmiFilter{
				{
					Name:   "name",
					Values: []string{"al2023-ami-2023*-kernel-6.12-x86_64"},
				},
				{
					Name:   "virtualization-type",
					Values: []string{"hvm"},
				},
				{
					Name:   "architecture",
					Values: []string{"x86_64"},
				},
			},
			Owners: []string{"amazon"},
		})
		if err != nil {
			return err
		}

		// Create an EC2 instance
		instance, err := ec2.NewInstance(ctx, "flink-ddb-instance", &ec2.InstanceArgs{
			InstanceType:        pulumi.String("t3.medium"),
			Ami:                 pulumi.String(ami.Id),
			SubnetId:            subnet.ID(),
			VpcSecurityGroupIds: pulumi.StringArray{sg.ID()},
			KeyName:             pulumi.String("keypair-sandbox0-sin-mymac.pem"),
			Tags: pulumi.StringMap{
				"Name": pulumi.String("flink-ddb-instance"),
			},
			UserData: pulumi.String(`#!/bin/bash
# Install Docker
sudo dnf update -y
sudo dnf install -y docker
sudo systemctl enable docker
sudo systemctl start docker
sudo usermod -aG docker ec2-user

# Install Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# Install Java 11
sudo dnf install -y java-11-amazon-corretto-devel

# Install Git
sudo dnf install -y git

# Create directory for Flink
mkdir -p /home/ec2-user/flink-ddb-lab
`),
		})
		if err != nil {
			return err
		}

		// Create a DynamoDB table for product catalog
		productTable, err := dynamodb.NewTable(ctx, "product-catalog", &dynamodb.TableArgs{
			Attributes: dynamodb.TableAttributeArray{
				&dynamodb.TableAttributeArgs{
					Name: pulumi.String("product_id"),
					Type: pulumi.String("S"),
				},
			},
			HashKey:     pulumi.String("product_id"),
			BillingMode: pulumi.String("PAY_PER_REQUEST"),
			TableClass:  pulumi.String("STANDARD"),
			Tags: pulumi.StringMap{
				"Name": pulumi.String("product_catalog"),
			},
		})
		if err != nil {
			return err
		}

		// Export the instance public IP and DynamoDB table name
		ctx.Export("instancePublicIp", instance.PublicIp)
		ctx.Export("productCatalogTableName", productTable.Name)
		ctx.Export("productCatalogTableArn", productTable.Arn)

		return nil
	})
}
