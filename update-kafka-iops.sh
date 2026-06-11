#!/bin/bash
# ==============================================================================
# AWS EBS IOPS Updater for Kafka
# This script uses pure AWS CLI to find volumes tagged with the Kafka PVC names
# and modifies them to 10,000 IOPS.
# Run this in AWS CloudShell. No kubectl required!
# ==============================================================================

set -e

TARGET_IOPS=10000

echo "Looking for Kafka EBS volumes in AWS..."
# AWS tags Kubernetes dynamically provisioned volumes with the PVC name.
# We search for any volume that has a tag value containing 'data-my-kafka-controller'
VOLUME_IDS=$(aws ec2 describe-volumes \
    --filters "Name=tag-value,Values=*data-my-kafka-controller*" \
    --query "Volumes[*].VolumeId" \
    --output text)

if [ -z "$VOLUME_IDS" ] || [ "$VOLUME_IDS" == "None" ]; then
    echo "No volumes found matching '*data-my-kafka-controller*' in tags."
    echo "Are you in the correct AWS region in CloudShell? (e.g. us-east-1)"
    exit 1
fi

echo "Found Kafka Volumes: $VOLUME_IDS"

for vol_id in $VOLUME_IDS; do
    echo "Modifying volume $vol_id to $TARGET_IOPS IOPS..."
    aws ec2 modify-volume --volume-id "$vol_id" --volume-type gp3 --iops "$TARGET_IOPS"
done

echo "=============================================================================="
echo "All volume modification requests have been sent."
echo "You can monitor the status using:"
echo "aws ec2 describe-volumes-modifications | grep -E 'VolumeId|ModificationState'"
echo "=============================================================================="
